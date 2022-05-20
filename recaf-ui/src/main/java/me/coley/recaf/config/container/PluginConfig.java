package me.coley.recaf.config.container;

import dev.xdark.recaf.plugin.PluginInformation;
import me.coley.recaf.config.ConfigContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * Config container for plugin values.
 * @author xtherk
 */
public class PluginConfig implements ConfigContainer {

    /**
     * Describe the switch status of the plugin
     */
    public Map<String, Boolean> descriptor = new HashMap<>();

    public void setEnabled(PluginInformation info, boolean isEnabled) {
        descriptor.put(info.getName(), isEnabled);
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
