package me.coley.recaf.plugin;

import me.coley.recaf.config.ConfKeybinding;
import me.coley.recaf.plugin.api.BasePlugin;
import me.coley.recaf.plugin.api.KeybindProviderPlugin;
import me.coley.recaf.ui.controls.view.ClassViewport;
import me.coley.recaf.ui.controls.view.FileViewport;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Keybind manager for plugins.
 *
 * @author Matt
 */
public class PluginKeybinds {
	private static final PluginKeybinds INSTANCE;
	private final Set<KeybindProviderPlugin> keybindProviders = new HashSet<>();
	private final Map<KeybindProviderPlugin, Map<ConfKeybinding.Binding, Runnable>>
			globalBinds = new HashMap<>();
	private final Map<KeybindProviderPlugin, Map<ConfKeybinding.Binding, Consumer<ClassViewport>>>
			classViewBinds = new HashMap<>();
	private final Map<KeybindProviderPlugin, Map<ConfKeybinding.Binding, Consumer<FileViewport>>>
			fileViewBinds = new HashMap<>();

	// Deny constructor
	private PluginKeybinds() {}

	/**
	 * Setup keybind collections.
	 */
	public void setup() {
		if (keybindProviders.isEmpty()) {
			Collection<KeybindProviderPlugin> keybindProviders = PluginsManager.getInstance()
					.ofType(KeybindProviderPlugin.class);
			globalBinds.putAll(keybindProviders.stream()
					.collect(Collectors.toMap(provider -> provider, KeybindProviderPlugin::createGlobalBindings)));
			classViewBinds.putAll(keybindProviders.stream()
					.collect(Collectors.toMap(provider -> provider, KeybindProviderPlugin::createClassViewBindings)));
			fileViewBinds.putAll(keybindProviders.stream()
					.collect(Collectors.toMap(provider -> provider, KeybindProviderPlugin::createFileViewBindings)));
		}
	}

	/**
	 * @return Active global binds and their actions.
	 */
	public Map<ConfKeybinding.Binding, Runnable> getGlobalBinds() {
		return globalBinds.entrySet().stream()
				.filter(p -> isActive(p.getKey()))
				.flatMap(p -> p.getValue().entrySet().stream())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/**
	 * @return Active class-view binds and their actions.
	 */
	public Map<ConfKeybinding.Binding, Consumer<ClassViewport>> getClassViewBinds() {
		return classViewBinds.entrySet().stream()
				.filter(p -> isActive(p.getKey()))
				.flatMap(p -> p.getValue().entrySet().stream())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/**
	 * @return Active file-view binds and their actions.
	 */
	public Map<ConfKeybinding.Binding, Consumer<FileViewport>> getFileViewBinds() {
		return fileViewBinds.entrySet().stream()
				.filter(p -> isActive(p.getKey()))
				.flatMap(p -> p.getValue().entrySet().stream())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/**
	 * @param plugin
	 * 		Plugin to check.
	 *
	 * @return {@code true} When the plugin state is active.
	 */
	private static boolean isActive(BasePlugin plugin) {
		return PluginsManager.getInstance().getPluginStates().getOrDefault(plugin.getName(), false);
	}

	/**
	 * @return Plugin keybind manager instance.
	 */
	public static PluginKeybinds getInstance() {
		return INSTANCE;
	}

	static {
		INSTANCE = new PluginKeybinds();
	}
}
