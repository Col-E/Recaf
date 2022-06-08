package me.coley.recaf.config.container;

import dev.xdark.recaf.plugin.PluginInformation;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.ConfigID;
import me.coley.recaf.config.Group;

import java.util.HashMap;
import java.util.Map;

/**
 * Config container for plugin values.
 *
 * @author xtherk
 */
public class PluginConfig implements ConfigContainer {
	/**
	 * Describe the switch status of the plugin.
	 */
	@Group("general")
	@ConfigID("enabled")
	public Map<String, Boolean> enabledState = new HashMap<>();

	/**
	 * Record the current enabled state of a given plugin.
	 *
	 * @param info
	 * 		Plugin information to pull name from.
	 * @param isEnabled
	 * 		Enabled state of the plugin.
	 */
	public void setEnabled(PluginInformation info, boolean isEnabled) {
		enabledState.put(info.getName(), isEnabled);
	}

	@Override
	public String iconPath() {
		return null;
	}

	@Override
	public String internalName() {
		return "conf.plugin";
	}

	@Override
	public boolean isInternal() {
		return true;
	}
}
