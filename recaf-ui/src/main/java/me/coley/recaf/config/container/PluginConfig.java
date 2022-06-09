package me.coley.recaf.config.container;

import dev.xdark.recaf.plugin.PluginInformation;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.ConfigID;
import me.coley.recaf.config.Group;
import me.coley.recaf.ui.plugin.item.RemotePluginItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Config container for plugin values.
 *
 * @author xtherk
 */
public class PluginConfig implements ConfigContainer {
	/**
	 * Status of each plugin.
	 */
	@Group("general")
	@ConfigID("enabled")
	public Map<String, Boolean> enabledState = new HashMap<>();
	/**
	 * Information about remote plugins. Used as a cache so the Github API isn't hit too often.
	 */
	@Group("general")
	@ConfigID("remote")
	public List<RemotePluginItem> cachedRemotePlugins = new ArrayList<>();
	/**
	 * Timestamp of data in {@link #cachedRemotePlugins}.
	 */
	@Group("general")
	@ConfigID("remotetime")
	public long cachedRemoteTime = -1;

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

	/**
	 * @param info
	 * 		Plugin information to pull name from.
	 *
	 * @return Enabled state of the plugin.
	 */
	public boolean isEnabled(PluginInformation info) {
		return enabledState.getOrDefault(info.getName(), false);
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
