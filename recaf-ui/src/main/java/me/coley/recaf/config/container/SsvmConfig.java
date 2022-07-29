package me.coley.recaf.config.container;

import dev.xdark.recaf.jdk.JavaUtil;
import me.coley.recaf.Controller;
import me.coley.recaf.RecafUI;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.ConfigID;
import me.coley.recaf.config.Group;
import me.coley.recaf.ssvm.LocalVmFactory;
import me.coley.recaf.ssvm.RemoteVmFactory;
import me.coley.recaf.ssvm.SsvmIntegration;
import me.coley.recaf.ssvm.VmFactory;
import me.coley.recaf.ui.util.Icons;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Config container for {@link me.coley.recaf.ssvm.SsvmIntegration}.
 *
 * @author Matt Coley
 */
public class SsvmConfig implements ConfigContainer {
	/**
	 * Flag to enable SSVM operations.
	 */
	@Group("general")
	@ConfigID("active")
	public boolean active = true;
	/**
	 * @see SsvmIntegration#doAllowRead()
	 */
	@Group("access")
	@ConfigID("read")
	public boolean allowRead;
	/**
	 * @see SsvmIntegration#doAllowWrite()
	 */
	@Group("access")
	@ConfigID("write")
	public boolean allowWrite;
	/**
	 * Flag to enable usage of {@link RemoteVmFactory} for {@link SsvmIntegration#getFactory()}.
	 */
	@Group("remote")
	@ConfigID("active")
	public boolean useRemote;
	/**
	 * Path to {@code java} executable to use in {@link RemoteVmFactory}.
	 */
	@Group("remote")
	@ConfigID("path")
	public String remoteJvmPath = initialJvmPath();

	@Override
	public void onLoad() {
		// Register listener to update SSVM integration access rules.
		RecafUI.getController().addListener((old, current) -> updateIntegration());
		// Reset if file path does not exist
		if (!doesJvmPathExist()) {
			remoteJvmPath = initialJvmPath();
		}
	}

	@Override
	public String iconPath() {
		return Icons.SMART;
	}

	@Override
	public String internalName() {
		return "conf.ssvm";
	}

	/**
	 * @return {@code true} if {@link #remoteJvmPath} points to a real file.
	 */
	private boolean doesJvmPathExist() {
		return Files.isRegularFile(Paths.get(remoteJvmPath));
	}

	/**
	 * Update {@link SsvmIntegration} with current access flags.
	 */
	public void updateIntegration() {
		if (!active)
			return;
		Controller controller = RecafUI.getController();
		SsvmIntegration ssvm = controller.getServices().getSsvmIntegration();
		if (ssvm != null) {
			// Update access restrictions
			ssvm.setAllowRead(allowRead);
			ssvm.setAllowWrite(allowWrite);
			// Update factory
			VmFactory factory;
			if (useRemote && doesJvmPathExist()) {
				factory = new RemoteVmFactory(Paths.get(remoteJvmPath));
			} else {
				factory = new LocalVmFactory(controller.getWorkspace());
			}
			boolean updated = ssvm.setFactory(factory);
			// If the impl hasn't changed and SSVM is not yet initialized, go ahead and request the VM be generated.
			if (!updated && !ssvm.isInitialized()) {
				ssvm.asyncUpdatePrimaryVm();
			}
		}
	}

	/**
	 * @return Current java executable path.
	 */
	private static String initialJvmPath() {
		return JavaUtil.getJavaExecutable().toAbsolutePath().toString();
	}
}
