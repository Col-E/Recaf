package me.coley.recaf.ssvm;

import dev.xdark.recaf.jdk.properties.JdkProperties;
import dev.xdark.recaf.jdk.resources.JdkResourcesServer;
import dev.xdark.ssvm.api.MethodInvoker;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.classloading.BootClassLoader;
import dev.xdark.ssvm.execution.Result;
import dev.xdark.ssvm.mirror.InstanceJavaClass;
import me.coley.recaf.ssvm.loader.RemoteBootClassLoader;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Factory implementation that provides workspace class access via a {@link JdkResourcesServer}.
 *
 * @author xDark
 * @author Matt Coley
 * @see LocalVmFactory
 */
public class RemoteVmFactory implements VmFactory {
	private static final Logger logger = Logging.get(RemoteVmFactory.class);
	private final Path remoteJavaExecutable;

	/**
	 * @param remoteJavaExecutable
	 * 		Path to {@code java} executable.
	 */
	public RemoteVmFactory(Path remoteJavaExecutable) {
		this.remoteJavaExecutable = remoteJavaExecutable;
	}

	@Override
	public IntegratedVirtualMachine create(SsvmIntegration integration) {
		JdkResourcesServer peer;
		try {
			peer = JdkResourcesServer.start(remoteJavaExecutable);
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to start remote server", ex);
		}
		IntegratedVirtualMachine vm = new IntegratedVirtualMachine() {
			@Override
			public void bootstrap() {
				super.bootstrap();
				// Workspace classes/files supplied via virtual zip/jar
				getVmUtil().addUrl(WorkspaceZipFile.RECAF_LIVE_ZIP);
			}

			@Override
			protected SsvmIntegration integration() {
				return integration;
			}

			@Override
			protected BootClassLoader createBootClassLoader() {
				return new RemoteBootClassLoader(peer);
			}
		};
		// Copy remote properties
		try {
			JdkProperties properties = JdkProperties.getProperties(remoteJavaExecutable);
			vm.getenv().clear();
			vm.getenv().putAll(properties.getEnvironment());
			vm.getProperties().clear();
			vm.getProperties().putAll(properties.getSystemProperties());
		} catch (IOException ex) {
			logger.warn("Failed to dump JDK properties", ex);
		}
		vm.initialize();
		VMInterface vmi = vm.getInterface();
		InstanceJavaClass cl = (InstanceJavaClass) vm.findBootstrapClass("java/lang/Shutdown");
		vmi.setInvoker(cl, "beforeHalt", "()V", ctx -> {
			try {
				peer.shutdown();
			} catch (IOException ex) {
				logger.error("Could not shutdown remote server", ex);
			}
			return Result.ABORT;
		});
		vmi.setInvoker(cl, "halt0", "(I)V", MethodInvoker.noop());
		return vm;
	}
}
