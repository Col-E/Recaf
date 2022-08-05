package me.coley.recaf.ssvm;

import dev.xdark.ssvm.execution.ExecutionContext;
import dev.xdark.ssvm.execution.VMException;
import dev.xdark.ssvm.mirror.InstanceJavaClass;
import dev.xdark.ssvm.value.InstanceValue;
import dev.xdark.ssvm.value.Value;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.ThreadPoolFactory;
import me.coley.recaf.workspace.Workspace;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Wrapper around SSVM to integrate with Recaf workspaces.
 *
 * @author Matt Coley
 */
public class SsvmIntegration {
	private static final Logger logger = Logging.get(SsvmIntegration.class);
	private static final ExecutorService vmThreadPool = ThreadPoolFactory.newFixedThreadPool("Recaf SSVM", 1, true);
	private final Workspace workspace;
	private VmFactory factory;
	private IntegratedVirtualMachine vm;
	private boolean initialized;
	private boolean allowRead;
	private boolean allowWrite;
	private Exception initializeError;

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	public SsvmIntegration(Workspace workspace) {
		this.workspace = workspace;
		this.factory = new LocalVmFactory(workspace);
	}

	/**
	 * Update the {@link #getVm() primary VM} on a background thread.
	 */
	public void asyncUpdatePrimaryVm() {
		vmThreadPool.submit(this::updatePrimaryVm);
	}

	/**
	 * Update the {@link #getVm() primary VM}.
	 */
	public void updatePrimaryVm() {
		try {
			if (vm != null) {
				SsvmUtil.shutdown(vm, 0);
			}
			vm = createVM(false, vmm -> {
				try {
					vmm.bootstrap();
					initialized = true;
				} catch (Exception ex) {
					initializeError = ex;
				}
			});
			onPostInit();
		} catch (Exception ex) {
			vm = null;
			initializeError = ex;
			onPostInit();
		}
	}

	private void onPostInit() {
		if (initialized) {
			logger.debug("SSVM initialized successfully");
		} else {
			Exception initializeError = this.initializeError;
			Throwable cause = initializeError.getCause();
			if (cause instanceof VMException) {
				IntegratedVirtualMachine vm = getVm();
				if (vm != null) {
					InstanceValue oop = ((VMException) cause).getOop();
					logger.error("SSVM failed to initialize: {}", oop);
					logger.error(vm.getVmUtil().throwableToString(oop));
					return;
				}
			}
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
	public IntegratedVirtualMachine createVM(boolean initialize, Consumer<IntegratedVirtualMachine> postInit) {
		IntegratedVirtualMachine vm = factory.create(this);
		if (initialize) {
			try {
				vm.bootstrap();
				vm.findBootstrapClass("jdk/internal/ref/CleanerFactory", true);
				if (postInit != null)
					postInit.accept(vm);
			} catch (Exception ex) {
				logger.error("Failed to initialize VM", ex);
			}
		} else {
			if (postInit != null)
				postInit.accept(vm);
		}
		return vm;
	}

	/**
	 * Run the method in the given VM.
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
	public static CompletableFuture<VmRunResult> runMethod(IntegratedVirtualMachine vm,
														   CommonClassInfo owner, MethodInfo method,
														   Value[] parameters) {
		InstanceJavaClass vmClass;
		try {
			vmClass = (InstanceJavaClass) vm.findClass(vm.getVmUtil().getSystemClassLoader(), owner.getName(), true);
		} catch (Exception ex) {
			// If the class isn't found we get 'null'.
			// If the class is found but cannot be loaded we probably get some error that we need to handle here.
			logger.warn("Failed to initialize class: {}", owner.getName());
			return CompletableFuture.completedFuture(new VmRunResult(ex));
		}
		// Invoke with parameters and return value
		return CompletableFuture.supplyAsync(() -> {
			int access = method.getAccess();
			String methodName = method.getName();
			String methodDesc = method.getDescriptor();
			try {
				vmClass.initialize();
				ExecutionContext context;
				if (AccessFlag.isStatic(access)) {
					context = vm.getVmUtil().invokeStatic(vmClass, methodName, methodDesc, parameters);
				} else {
					context = vm.getVmUtil().invokeExact(vmClass, methodName, methodDesc, parameters);
				}
				return new VmRunResult(context.getResult());
			} catch (Exception ex) {
				// Also handles 'VMException'
				return new VmRunResult(ex);
			}
		}, vmThreadPool);
	}

	/**
	 * @return Current VM instance.
	 */
	public IntegratedVirtualMachine getVm() {
		if (vm == null)
			updatePrimaryVm();
		return vm;
	}

	/**
	 * @return Associated workspace.
	 */
	public Workspace getWorkspace() {
		return workspace;
	}

	/**
	 * @return {@code true} whenb the VM is ready.
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * @return Current VM instance factory.
	 */
	public VmFactory getFactory() {
		return factory;
	}

	/**
	 * @param factory
	 * 		New VM instance factory.
	 *
	 * @return {@code true} when the VM factory was changed.
	 * {@code false} indicates the same factory implementation as the current was passed.
	 */
	public boolean setFactory(VmFactory factory) {
		// Only change if the implementation is different
		if (this.factory.getClass() != factory.getClass()) {
			this.factory = factory;
			asyncUpdatePrimaryVm();
			return true;
		}
		return false;
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
	 * @return {@code true} when the VM should have access to host user's file system for reading.
	 * {@code false} provides the VM an empty file instead.
	 */
	public boolean doAllowRead() {
		return allowRead;
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
	 * {@code false} by default.
	 *
	 * @return {@code true} when the VM should have access to host user's file system for writing.
	 * {@code false} provides a no-op write behavior.
	 */
	public boolean doAllowWrite() {
		return allowWrite;
	}

	/**
	 * Shutdowns VM.
	 */
	public void cleanup() {
		IntegratedVirtualMachine vm = this.vm;
		if (vm != null) {
			SsvmUtil.shutdown(vm, 0);
		}
	}
}
