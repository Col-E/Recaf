package me.coley.recaf.ssvm;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.classloading.BootClassLoader;
import dev.xdark.ssvm.classloading.CompositeBootClassLoader;
import dev.xdark.ssvm.execution.ExecutionContext;
import dev.xdark.ssvm.fs.FileDescriptorManager;
import dev.xdark.ssvm.mirror.InstanceJavaClass;
import dev.xdark.ssvm.util.VMHelper;
import dev.xdark.ssvm.value.Value;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.ssvm.loader.RuntimeBootClassLoader;
import me.coley.recaf.ssvm.loader.WorkspaceBootClassLoader;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.ThreadPoolFactory;
import me.coley.recaf.workspace.Workspace;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;

/**
 * Wrapper around SSVM to integrate with Recaf workspaces.
 *
 * @author Matt Coley
 */
public class SsvmIntegration {
	private static final Value[] EMPTY_STACK = new Value[0];
	private static final Logger logger = Logging.get(SsvmIntegration.class);
	private static final ExecutorService vmThreadPool = ThreadPoolFactory.newFixedThreadPool("Recaf SSVM");
	private final VirtualMachine vm;
	private boolean initialized;
	private Exception initializeError;

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	public SsvmIntegration(Workspace workspace) {
		vm = new VirtualMachine() {
			@Override
			protected FileDescriptorManager createFileDescriptorManager() {
				return new DenyingFileDescriptorManager();
			}

			@Override
			protected BootClassLoader createBootClassLoader() {
				return new CompositeBootClassLoader(Arrays.asList(
						new WorkspaceBootClassLoader(workspace),
						new RuntimeBootClassLoader()
				));
			}
		};
		vmThreadPool.execute(() -> {
			try {
				vm.bootstrap();
				initialized = true;
			} catch (Exception ex) {
				initializeError = ex;
			}
			onPostInit();
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
	 * @return {@code true} whenb the VM is ready.
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * @param owner
	 * 		Class declaring the method.
	 * @param method
	 * 		Method to invoke in the VM.
	 *
	 * @return Result of invoke.
	 */
	public VmRunResult runMethod(CommonClassInfo owner, MethodInfo method) {
		InstanceJavaClass vmClass = (InstanceJavaClass) vm.findBootstrapClass(owner.getName());
		if (vmClass == null) {
			return new VmRunResult(new IllegalStateException("Class not found in VM: " + owner.getName()));
		}

		VMHelper helper = vm.getHelper();
		int access = method.getAccess();
		String methodName = method.getName();
		String methodDesc = method.getDescriptor();
		// TODO: We are in the core module, so how should we go about asking users for input?
		//  - add a `Value[]` parameter which is populated by callers in UI module
		//  -
		Value[] parameters = new Value[0];
		// Invoke with parameters and return value
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
	}


	/**
	 * Wrapper around a VM return value, or an exception if the VM could not execute.
	 */
	public static class VmRunResult {
		private Exception exception;
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
		public VmRunResult(Exception exception) {
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
		public Exception getException() {
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
