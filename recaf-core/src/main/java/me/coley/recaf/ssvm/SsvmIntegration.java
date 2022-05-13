package me.coley.recaf.ssvm;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.classloading.BootClassLoader;
import dev.xdark.ssvm.classloading.CompositeBootClassLoader;
import dev.xdark.ssvm.execution.ExecutionContext;
import dev.xdark.ssvm.execution.VMException;
import dev.xdark.ssvm.fs.FileDescriptorManager;
import dev.xdark.ssvm.fs.HostFileDescriptorManager;
import dev.xdark.ssvm.mirror.InstanceJavaClass;
import dev.xdark.ssvm.mirror.JavaClass;
import dev.xdark.ssvm.util.VMHelper;
import dev.xdark.ssvm.value.ArrayValue;
import dev.xdark.ssvm.value.InstanceValue;
import dev.xdark.ssvm.value.Value;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.ssvm.loader.RuntimeBootClassLoader;
import me.coley.recaf.ssvm.loader.WorkspaceBootClassLoader;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.ThreadPoolFactory;
import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.Type;
import org.slf4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Wrapper around SSVM to integrate with Recaf workspaces.
 *
 * @author Matt Coley
 */
public class SsvmIntegration {
	private static final Value[] EMPTY_STACK = new Value[0];
	private static final Logger logger = Logging.get(SsvmIntegration.class);
	private static final ExecutorService vmThreadPool = ThreadPoolFactory.newFixedThreadPool("Recaf SSVM");
	private final Workspace workspace;
	private VirtualMachine vm;
	private boolean initialized;
	private Exception initializeError;
	private boolean allowRead;
	private boolean allowWrite;

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	public SsvmIntegration(Workspace workspace) {
		this.workspace = workspace;
		vmThreadPool.submit(() -> {
			try {
				vm = createVM(false, null);
				try {
					vm.bootstrap();
					initialized = true;
				} catch (Exception ex) {
					initializeError = ex;
				}
				onPostInit();
			} catch (Exception ex) {
				vm = null;
				initializeError = ex;
				onPostInit();
			}
		});
	}

	private void onPostInit() {
		if (initialized) {
			logger.debug("SSVM initialized successfully");
		} else {
			logger.error("SSVM failed to initialize", initializeError);
		}
	}

	/**
	 * @param initialize
	 * 		Schedule VM initialization for the created VM.
	 * @param postInit
	 * 		Optional action to run after initialization.
	 *
	 * @return New VM.
	 */
	public VirtualMachine createVM(boolean initialize, Consumer<VirtualMachine> postInit) {
		VirtualMachine vm = new VirtualMachine() {
			@Override
			protected FileDescriptorManager createFileDescriptorManager() {
				return new HostFileDescriptorManager() {
					@Override
					public long open(String path, int mode) throws IOException {
						long fd = newFD();
						if ((allowRead && mode == READ) || (allowWrite && (mode == WRITE || mode == APPEND)))
							logger.trace("VM file handle[{}:{}]: {}",
									VirtualMachineUtil.describeFileMode(mode), fd, path);
						switch (mode) {
							case READ: {
								InputStream in;
								if (allowRead)
									in = new FileInputStream(path);
								else
									in = new ByteArrayInputStream(new byte[0]);
								inputs.put(fd, in);
								return fd;
							}
							case WRITE:
							case APPEND: {
								OutputStream out;
								if (allowWrite)
									out = new FileOutputStream(path, mode == APPEND);
								else
									out = new ByteArrayOutputStream();
								outputs.put(fd, out);
								return fd;
							}
							default:
								throw new IOException("Unknown mode: " + mode);
						}
					}
				};
			}

			@Override
			protected BootClassLoader createBootClassLoader() {
				return new CompositeBootClassLoader(Arrays.asList(
						new WorkspaceBootClassLoader(workspace),
						new RuntimeBootClassLoader()
				));
			}
		};
		if (initialize)
			try {
				vm.bootstrap();
				if (postInit != null)
					postInit.accept(vm);
			} catch (Exception ex) {
				logger.error("Failed to initialize VM", ex);
			}
		return vm;
	}

	/**
	 * @return Current VM instance.
	 */
	public VirtualMachine getVm() {
		return vm;
	}

	/**
	 * @return {@code true} whenb the VM is ready.
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * {@code false} by default.
	 *
	 * @return {@code true} when the VM should have access to host user's file system for reading.
	 * {@code false} provides the VM an empty file instead.
	 */
	public boolean doAllowRead() {
		return allowRead;
	}

	/**
	 * {@code false} by default.
	 *
	 * @param allowRead
	 *        {@code true} when the VM should have access to host user's file contents.
	 *        {@code false} provides the VM an empty file instead.
	 */
	public void setAllowRead(boolean allowRead) {
		this.allowRead = allowRead;
	}

	/**
	 * {@code false} by default.
	 *
	 * @return {@code true} when the VM should have access to host user's file system for writing.
	 * {@code false} provides a no-op write behavior.
	 */
	public boolean doAllowWrite() {
		return allowWrite;
	}

	/**
	 * {@code false} by default.
	 *
	 * @param allowWrite
	 *        {@code true} when the VM should have access to host user's file system for writing.
	 *        {@code false} provides a no-op write behavior.
	 */
	public void setAllowWrite(boolean allowWrite) {
		this.allowWrite = allowWrite;
	}

	/**
	 * Run the method in the {@link #getVm() current VM}.
	 *
	 * @param owner
	 * 		Class declaring the method.
	 * @param method
	 * 		Method to invoke in the VM.
	 * @param parameters
	 * 		Parameter values to pass.
	 *
	 * @return Result of invoke.
	 */
	public CompletableFuture<VmRunResult> runMethod(CommonClassInfo owner, MethodInfo method, Value[] parameters) {
		return runMethod(getVm(), owner, method, parameters);
	}

	/**
	 * Run the method in the given VM.
	 *
	 * @param vm
	 * 		Target VM to run in.
	 * @param owner
	 * 		Class declaring the method.
	 * @param method
	 * 		Method to invoke in the VM.
	 * @param parameters
	 * 		Parameter values to pass.
	 *
	 * @return Result of invoke.
	 */
	public CompletableFuture<VmRunResult> runMethod(VirtualMachine vm, CommonClassInfo owner, MethodInfo method, Value[] parameters) {
		InstanceJavaClass vmClass = (InstanceJavaClass) vm.findBootstrapClass(owner.getName());
		if (vmClass == null) {
			return CompletableFuture.completedFuture(
					new VmRunResult(new IllegalStateException("Class not found in VM: " + owner.getName())));
		}
		// Invoke with parameters and return value
		return CompletableFuture.supplyAsync(() -> {
			VMHelper helper = vm.getHelper();
			int access = method.getAccess();
			String methodName = method.getName();
			String methodDesc = method.getDescriptor();
			try {
				if (vmClass.shouldBeInitialized())
					vmClass.initialize();
				ExecutionContext context;
				if (AccessFlag.isStatic(access)) {
					context = helper.invokeStatic(vmClass, methodName, methodDesc,
							EMPTY_STACK,
							parameters);
				} else {
					context = helper.invokeExact(vmClass, methodName, methodDesc,
							EMPTY_STACK,
							parameters);
				}
				return new VmRunResult(context.getResult());
			} catch (Exception ex) {
				return new VmRunResult(ex);
			}
		}, vmThreadPool);
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return String representation.
	 */
	public String toString(Value value) {
		String valueString = null;
		if (value.isNull()) {
			return "null";
		} else if (value instanceof InstanceValue) {
			InstanceValue instance = (InstanceValue) value;
			if (instance.getJavaClass().getInternalName().equals("java/lang/String")) {
				valueString = vm.getHelper().readUtf8(value);
			}
		} else if (value instanceof ArrayValue) {
			ArrayValue array = (ArrayValue) value;
			JavaClass cls = array.getJavaClass();
			Type arrayType = Type.getType(cls.getDescriptor());
			int length = array.getLength();
			List<String> parts = new ArrayList<>();
			Type element = arrayType.getElementType();
			switch (element.getSort()) {
				case Type.BOOLEAN:
					for (int i = 0; i < length; i++)
						parts.add(String.valueOf(array.getBoolean(i)));
					break;
				case Type.CHAR:
					for (int i = 0; i < length; i++)
						parts.add(String.valueOf(array.getChar(i)));
					break;
				case Type.BYTE:
					for (int i = 0; i < length; i++)
						parts.add(String.valueOf(array.getByte(i)));
					break;
				case Type.SHORT:
					for (int i = 0; i < length; i++)
						parts.add(String.valueOf(array.getShort(i)));
					break;
				case Type.INT:
					for (int i = 0; i < length; i++)
						parts.add(String.valueOf(array.getInt(i)));
					break;
				case Type.FLOAT:
					for (int i = 0; i < length; i++)
						parts.add(String.valueOf(array.getFloat(i)));
					break;
				case Type.LONG:
					for (int i = 0; i < length; i++)
						parts.add(String.valueOf(array.getLong(i)));
					break;
				case Type.DOUBLE:
					for (int i = 0; i < length; i++)
						parts.add(String.valueOf(array.getDouble(i)));
					break;
				case Type.OBJECT:
					for (int i = 0; i < length; i++)
						parts.add(toString(array.getValue(i)));
					break;
				default:
					throw new IllegalStateException("Unsupported element type: " + element);
			}
			valueString = "[" + String.join(", ", parts) + "]";
		}
		if (valueString == null)
			valueString = value.toString();
		return valueString;
	}

	/**
	 * @param ex
	 * 		Exception to print that may be virtualized <i>({@link VMException})</i>
	 *
	 * @return Unwrapped exception.
	 */
	public Throwable unwrap(Throwable ex) {
		if (ex instanceof VMException)
			ex = vm.getHelper().toJavaException(((VMException) ex).getOop());
		return ex;
	}

	/**
	 * Wrapper around a VM return value, or an exception if the VM could not execute.
	 */
	public static class VmRunResult {
		private Throwable exception;
		private Value value;

		/**
		 * @param value
		 * 		Execution return value.
		 */
		public VmRunResult(Value value) {
			this.value = value;
		}

		/**
		 * @param exception
		 * 		Execution failure.
		 */
		public VmRunResult(Throwable exception) {
			this.exception = exception;
		}

		/**
		 * @return Execution return value.
		 */
		public Value getValue() {
			return value;
		}

		/**
		 * @return Execution failure.
		 */
		public Throwable getException() {
			return exception;
		}

		/**
		 * @return {@code true} when there is an {@link #getException() error}.
		 */
		public boolean hasError() {
			return exception != null;
		}
	}
}
