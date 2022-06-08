package me.coley.recaf.util;

import dev.xdark.recaf.plugin.RecafPlugin;
import me.coley.recaf.config.Configs;

import java.util.Map;
import java.util.stream.Collectors;

public class PluginLoader {

    public static void install() {
        // Turn on the plugin that needs to be enabled
        RecafPlugin.getInstance().enablePlugins(Configs.plugin().descriptor
                .entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet())
        );
    }
}
