package software.coley.recaf.services.vm;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.api.MethodInvoker;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.classloading.BootClassFinder;
import dev.xdark.ssvm.classloading.ParsedClassData;
import dev.xdark.ssvm.classloading.SupplyingClassLoader;
import dev.xdark.ssvm.execution.Interpreter;
import dev.xdark.ssvm.execution.Result;
import dev.xdark.ssvm.filesystem.DelegatingFileManager;
import dev.xdark.ssvm.filesystem.FileManager;
import dev.xdark.ssvm.filesystem.HostFileManager;
import dev.xdark.ssvm.filesystem.SimpleFileManager;
import dev.xdark.ssvm.invoke.InvocationUtil;
import dev.xdark.ssvm.mirror.type.InstanceClass;
import dev.xdark.ssvm.operation.VMOperations;
import dev.xdark.ssvm.timezone.SimpleTimeManager;
import dev.xdark.ssvm.timezone.TimeManager;
import dev.xdark.ssvm.util.ClassUtil;
import dev.xdark.ssvm.value.InstanceValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import software.coley.classserver.JdkProperties;
import software.coley.classserver.JdkResourcesServer;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.services.Service;
import software.coley.recaf.util.ReflectUtil;
import software.coley.recaf.util.io.BufferDelegatingPrintStream;
import software.coley.recaf.workspace.WorkspaceManager;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import static dev.xdark.ssvm.classloading.SupplyingClassLoaderInstaller.*;

/**
 * Common service housing our SSVM {@link VirtualMachine} instance for other services to operate off of.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class CommonVirtualService implements Service {
	public static final String SERVICE_ID = "virtual-common";
	private static final DebuggingLogger logger = Logging.get(CommonVirtualService.class);
	private final CommonVirtualServiceConfig config;
	// Current VM data
	private int refreshCount;
	private CommonVM vm;
	private Throwable vmInitFailure;
	// Current workspace data on top of the VM
	private Workspace currentWorkspace;
	private Helper currentWorkspaceHelper;
	private Throwable currentWorkspaceHelperInitFailure;

	@Inject
	public CommonVirtualService(@Nonnull CommonVirtualServiceConfig config, @Nonnull WorkspaceManager workspaceManager) {
		this.config = config;
		this.currentWorkspace = workspaceManager.getCurrent();
		addWorkspaceListeners(workspaceManager);
		addConfigListeners();
	}

	/**
	 * Adds listeners to track the current workspace as unique {@link SupplyingClassLoader} instances.
	 *
	 * @param workspaceManager
	 * 		Workspace manager to register within.
	 */
	private void addWorkspaceListeners(@Nonnull WorkspaceManager workspaceManager) {
		workspaceManager.addWorkspaceOpenListener(workspace -> {
			currentWorkspace = workspace;
			currentWorkspaceHelperInitFailure = null;
		});
		workspaceManager.addWorkspaceCloseListener(workspace -> {
			currentWorkspaceHelper = null;
			currentWorkspaceHelperInitFailure = null;
		});
	}

	/**
	 * Adds listeners to config values which update a few VM states.
	 */
	private void addConfigListeners() {
		config.useAlternateJdkBootPath().addChangeListener((observable, old, current) -> {
			// If we have a VM already, make sure the refresh only happens where it makes sense.
			if (vm != null) {
				// Check if the boot path flag is the same.
				if (vm.useAltBootPath == current)
					return;

				// Check if the boot path itself is the same.
				if (Objects.equals(vm.altBootPath, config.getAlternateJdkPath().getValue()))
					return;
			}
			refreshVm();
		});
		config.getFakeClasspath().addChangeListener(((observable, old, current) -> {
			if (vm != null)
				vm.getProperties().put("java.class.path", current);
		}));
		config.getMaxInterpreterStepsPerMethod().addChangeListener(((observable, old, current) -> {
			Interpreter.setMaxIterations(current);
		}));
		Interpreter.setMaxIterations(config.getMaxInterpreterStepsPerMethod().getValue());
	}

	/**
	 * @return Helper for loading content from the {@link Workspace} into the current {@link VirtualMachine}.
	 * May be {@code null} if there is no current workspace, or creating the helper for the workspace encountered an error.
	 */
	@Nullable
	public Helper getCurrentWorkspaceHelper() {
		// Got an existing helper? Return it.
		if (currentWorkspaceHelper != null)
			return currentWorkspaceHelper;

		// No current workspace, so no helper.
		if (currentWorkspace == null)
			return null;

		// Creating a workspace helper for the current workspace failed, so abort.
		if (currentWorkspaceHelperInitFailure != null)
			return null;

		// Create and return the helper.
		try {
			return currentWorkspaceHelper = createWorkspaceHelper(currentWorkspace);
		} catch (Throwable t) {
			currentWorkspaceHelper = null;
			currentWorkspaceHelperInitFailure = t;
			logger.warn("Failed to create associated workspace", t);
			return null;
		}
	}

	/**
	 * Creates a class-loader helper in the {@link #getSharedVm() shared VM}.
	 *
	 * @param workspace
	 * 		Workspace to create a content loading helper for.
	 *
	 * @return Helper assisting in classloading and resource loading from the given workspace.
	 *
	 * @throws IOException
	 * 		When installing the helper in the current VM failed.
	 * @throws VmUnavailableException
	 * 		When there is no currently accessible shared VM.
	 */
	@Nonnull
	public Helper createWorkspaceHelper(@Nonnull Workspace workspace) throws IOException, VmUnavailableException {
		return createWorkspaceHelper(getSharedVm(), workspace);
	}

	/**
	 * Creates a class-loader helper in specified VM.
	 *
	 * @param targetVm
	 * 		The VM to create the workspace helper within.
	 * @param workspace
	 * 		Workspace to create a content loading helper for.
	 *
	 * @return Helper assisting in classloading and resource loading from the given workspace.
	 *
	 * @throws IOException
	 * 		When installing the helper in the given VM failed.
	 */
	@Nonnull
	public Helper createWorkspaceHelper(@Nonnull VirtualMachine targetVm, @Nonnull Workspace workspace) throws IOException {
		Function<String, byte[]> classProvider = className -> {
			ClassPathNode path = workspace.findJvmClass(className.replace('.', '/'));
			if (path != null)
				return path.getValue().asJvmClass().getBytecode();
			return null;
		};
		Function<String, byte[]> fileProvider = filePath -> {
			FilePathNode path = workspace.findFile(filePath);
			if (path != null)
				return path.getValue().getRawContent();
			return null;
		};
		return install(targetVm, supplyFromFunctions(classProvider, fileProvider));
	}

	/**
	 * Regenerate the shared VM instance.
	 *
	 * @return {@code true} on success, {@code false} on failure.
	 */
	public boolean refreshVm() {
		refreshCount++;
		Throwable cvmInitFail = null;
		CommonVM cvm;
		try {
			cvm = new CommonVM();
			cvm.bootstrap();
		} catch (Throwable t) {
			cvm = null;
			cvmInitFail = t;
			logger.warn("Failed to initialize VM. VM based abilities will be unavailable.");
		}
		vm = cvm;
		vmInitFailure = cvmInitFail;
		return vm != null;
	}

	/**
	 * Creates a new {@link CommonVM}. The created value will not synchronize with {@link CommonVirtualServiceConfig}.
	 * It will be configured only with what the current values are.
	 * <p>
	 * The {@link #getSharedVm() shared VM instance} will maintain value alignment with {@link CommonVirtualServiceConfig}.
	 *
	 * @return New VM instance.
	 *
	 * @throws Throwable
	 * 		When bootstrapping the VM fails.
	 */
	@Nonnull
	public CommonVM newVm() throws Throwable {
		CommonVM cvm = new CommonVM();
		cvm.bootstrap();
		return cvm;
	}

	/**
	 * The current shared VM maintained by this service. Can be regenerated via {@link #refreshVm()}.
	 * Properties pertaining to VM config from {@link CommonVirtualServiceConfig} are maintained within this VM instance.
	 *
	 * @return Current VM instance.
	 *
	 * @throws VmUnavailableException
	 * 		Thrown when not available.
	 */
	@Nonnull
	public CommonVM getSharedVm() throws VmUnavailableException {
		// If the VM hasn't been requested yet, initialize it.
		if (refreshCount == 0)
			refreshVm();

		// Get VM, or throw if not available.
		if (vm == null)
			throw new VmUnavailableException(vmInitFailure);
		return vm;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public CommonVirtualServiceConfig getServiceConfig() {
		return config;
	}

	static {
		// Required for some SSVM operations.
		ReflectUtil.patchAccess();
	}

	/**
	 * VM extension to plug our config values into.
	 */
	public class CommonVM extends VirtualMachine {
		private final InvocationUtil invocationUtil = InvocationUtil.create(this);
		private final boolean useAltBootPath = config.useAlternateJdkBootPath().getValue();
		private final String altBootPath = config.getAlternateJdkPath().getValue();
		private BufferDelegatingPrintStream out;
		private BufferDelegatingPrintStream err;

		@Override
		public void bootstrap() {
			super.bootstrap();

			// Hide class path of Recaf from the VM.
			getProperties().put("java.class.path", config.getFakeClasspath().getValue());

			// Some patches to circumvent bugs arising from VM implementation changes in later versions.
			VMInterface vmi = getInterface();
			VMOperations ops = getOperations();
			if (getJvmVersion() > 8) {
				// Bug in SSVM makes it think there are overlapping sleeps, so until that gets fixed we stub out sleeping.
				InstanceClass thread = getSymbols().java_lang_Thread();
				vmi.setInvoker(thread.getMethod("sleep", "(J)V"), MethodInvoker.noop());

				// SSVM manages its own memory, and this conflicts with it. Stubbing it out keeps everyone happy.
				InstanceClass bits = (InstanceClass) findBootstrapClass("java/nio/Bits");
				if (bits != null) vmi.setInvoker(bits.getMethod("reserveMemory", "(JJ)V"), MethodInvoker.noop());
			}

			// Hook/disable VM internal shutdown handling.
			InstanceClass cl = (InstanceClass) findBootstrapClass("java/lang/Shutdown");
			if (cl != null) {
				vmi.setInvoker(cl, "beforeHalt", "()V", MethodInvoker.noop());
				vmi.setInvoker(cl, "halt0", "(I)V", ctx -> {
					logger.debugging(l -> {
						int code = ctx.getLocals().loadInt(0);
						InstanceValue exception = ops.newException(getSymbols().java_lang_Exception());
						l.info("Hit VM shutdown hook with code {}", code, ops.toJavaException(exception));
					});
					return Result.ABORT;
				});
			}
		}

		/**
		 * Cannot be called until after the VM is constructed.
		 *
		 * @param outStreamConsumer
		 * 		Consumer to handle line-by-line text for {@link System#out}
		 * 		when {@link CommonVirtualServiceConfig#useHostFileManager()} is active.
		 */
		public void setOutStreamConsumer(@Nullable Consumer<String> outStreamConsumer) {
			out.setLineHandler(outStreamConsumer);
		}

		/**
		 * Cannot be called until after the VM is constructed.
		 *
		 * @param errStreamConsumer
		 * 		Consumer to handle line-by-line text for {@link System#err}
		 * 		when {@link CommonVirtualServiceConfig#useHostFileManager()} is active.
		 */
		public void setErrStreamConsumer(@Nullable Consumer<String> errStreamConsumer) {
			err.setLineHandler(errStreamConsumer);
		}

		/**
		 * @return Invocation helper.
		 */
		@Nonnull
		public InvocationUtil getInvocationUtil() {
			return invocationUtil;
		}

		@Override
		protected BootClassFinder createBootClassFinder() {
			if (config.useAlternateJdkBootPath().getValue()) {
				Path jdkPath = Paths.get(config.getAlternateJdkPath().getValue());
				try {
					// SSVM uses the VM properties to detect what version it is targeting.
					// We must fetch the properties and update our values to match.
					JdkProperties remoteProperties = JdkProperties.getProperties(jdkPath);
					var env = getenv();
					var properties = getProperties();
					env.clear();
					env.putAll(remoteProperties.getEnvironment());
					properties.clear();
					properties.putAll(remoteProperties.getSystemProperties());

					// Initialize the resource server and create a class finder with it.
					JdkResourcesServer server = JdkResourcesServer.start(jdkPath);
					return name -> {
						try {
							byte[] bytes = server.requestResource(name + ".class");
							ClassReader cr = new ClassReader(bytes);
							ClassNode node = ClassUtil.readNode(cr);
							return new ParsedClassData(cr, node);
						} catch (Throwable t) {
							return null;
						}
					};
				} catch (IOException ex) {
					logger.error("Failed to create remote server for JDK classes using '{}'", jdkPath, ex);
				}
			}

			return super.createBootClassFinder();
		}

		@Override
		protected FileManager createFileManager() {
			// Must be initialized here since this is called from the super-type's constructor
			out = new BufferDelegatingPrintStream();
			err = new BufferDelegatingPrintStream();

			SimpleFileManager dummyManager = new SimpleFileManager();
			HostFileManager hostManager = new HostFileManager(System.in, out, err) {
				private boolean allow(int mode) {
					return (config.hostAllowRead().getValue() && mode == READ) ||
							(config.hostAllowWrite().getValue() && mode != READ);
				}

				@Override
				public synchronized long open(String path, int mode) throws IOException {
					if (allow(mode))
						return super.open(path, mode);
					else
						return dummyManager.open(path, mode);
				}

				@Override
				public boolean delete(String path) {
					if (allow(WRITE))
						return super.delete(path);
					else
						return dummyManager.delete(path);
				}

				@Override
				public boolean setPermission(String path, int flag, boolean value, boolean ownerOnly) {
					if (allow(WRITE))
						return super.setPermission(path, flag, value, ownerOnly);
					else
						return dummyManager.setPermission(path, flag, value, ownerOnly);
				}

				@Override
				public boolean setLastModifiedTime(String path, long time) {
					if (allow(WRITE))
						return super.setLastModifiedTime(path, time);
					else
						return dummyManager.setLastModifiedTime(path, time);
				}

				@Override
				public boolean setReadOnly(String path) {
					if (allow(WRITE))
						return super.setReadOnly(path);
					else
						return dummyManager.setReadOnly(path);
				}

				@Override
				public long getSpace(String path, int id) {
					if (allow(READ))
						return super.getSpace(path, id);
					else
						return dummyManager.getSpace(path, id);
				}

				@Override
				public boolean createFileExclusively(String path) throws IOException {
					if (allow(WRITE))
						return super.createFileExclusively(path);
					else
						return dummyManager.createFileExclusively(path);
				}

				@Override
				public <A extends BasicFileAttributes> A getAttributes(String path, Class<A> attrType, LinkOption... options) throws IOException {
					if (allow(READ))
						return super.getAttributes(path, attrType, options);
					else
						return dummyManager.getAttributes(path, attrType, options);
				}

				@Override
				public String[] list(String path) {
					if (allow(READ))
						return super.list(path);
					else
						return dummyManager.list(path);
				}

				@Override
				public synchronized long openZipFile(String path, int mode) throws IOException {
					if (allow(READ))
						return super.openZipFile(path, mode);
					else
						return dummyManager.openZipFile(path, mode);
				}
			};
			return new DelegatingFileManager(() -> config.useHostFileManager().getValue() ? hostManager : dummyManager);
		}

		@Override
		protected TimeManager createTimeManager() {
			return new SimpleTimeManager() {
				@Override
				public long currentTimeMillis() {
					return super.currentTimeMillis() + config.getTimeOffsetMillis().getValue();
				}

				@Override
				public long nanoTime() {
					return super.nanoTime() + (config.getTimeOffsetMillis().getValue() * 1000000L);
				}
			};
		}
	}
}