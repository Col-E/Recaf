package me.coley.recaf.config.container;

import dev.xdark.recaf.jdk.JavaUtil;
import me.coley.recaf.Controller;
import me.coley.recaf.RecafUI;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.ConfigID;
import me.coley.recaf.config.Group;
import me.coley.recaf.ssvm.SsvmIntegration;
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
	 *
	 */
	@Group("remotejvm")
	@ConfigID("useremote")
	public boolean useRemote;

	@Group("remotejvm")
	@ConfigID("path")
	public String remoteJvmPath = initialJvmPath();

	@Override
	public void onLoad() {
		// Register listener to update SSVM integration access rules.
		RecafUI.getController().addListener((old, current) -> updateAccess());
		// Reset if file path does not exist
		if (!Files.exists(Paths.get(remoteJvmPath))) {
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
	 * Update {@link SsvmIntegration} with current access flags.
	 */
	public void updateAccess() {
		Controller controller = RecafUI.getController();
		SsvmIntegration ssvm = controller.getServices().getSsvmIntegration();
		if (ssvm != null) {
			ssvm.setAllowRead(allowRead);
			ssvm.setAllowWrite(allowWrite);
		}
	}

	/**
	 * @return Current java executable path.
	 */
	private static String initialJvmPath() {
		return JavaUtil.getJavaExecutable().toAbsolutePath().toString();
	}
}
