package me.coley.recaf.ssvm;

import dev.xdark.recaf.jdk.resources.JdkResourcesServer;
import dev.xdark.ssvm.classloading.BootClassLoader;
import me.coley.recaf.ssvm.loader.RemoteBootClassLoader;

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
		return new IntegratedVirtualMachine() {
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
				try {
					return new RemoteBootClassLoader(JdkResourcesServer.start(remoteJavaExecutable));
				} catch (IOException ex) {
					throw new IllegalStateException("Failed to create remote boot class loader", ex);
				}
			}
		};
	}
}
