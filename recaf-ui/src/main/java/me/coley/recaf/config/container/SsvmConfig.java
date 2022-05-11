package me.coley.recaf.config.container;

import me.coley.recaf.Controller;
import me.coley.recaf.RecafUI;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.ConfigID;
import me.coley.recaf.config.Group;
import me.coley.recaf.ssvm.SsvmIntegration;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.util.threading.ThreadUtil;

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
	public boolean allowRead; // TODO: Warn user when they toggle this to 'true'
	/**
	 * @see SsvmIntegration#doAllowWrite()
	 */
	@Group("access")
	@ConfigID("write")
	public boolean allowWrite;  // TODO: Warn user when they toggle this to 'true'

	@Override
	public void onLoad() {
		// TODO: If we change the UI control for the above to include warnings, we may as well have them
		//  live update the 'SsvmIntegration' instance, and then we can remove this logic here.
		Runnable updateTask = () -> {
			Controller controller = RecafUI.getController();
			SsvmIntegration ssvm = controller.getServices().getSsvmIntegration();
			ssvm.setAllowRead(allowRead);
			ssvm.setAllowWrite(allowWrite);
		};
		// Register listener to update SSVM integration access rules.
		ThreadUtil.runDelayed(1000, () -> {
			RecafUI.getController().addListener((old, current) -> updateTask.run());
			updateTask.run();
		});
	}

	@Override
	public String iconPath() {
		return Icons.SMART;
	}

	@Override
	public String internalName() {
		return "conf.ssvm";
	}
}
