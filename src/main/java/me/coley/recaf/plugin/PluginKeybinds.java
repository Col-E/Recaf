package me.coley.recaf.plugin;

import javafx.scene.input.KeyEvent;
import me.coley.recaf.config.ConfKeybinding;
import me.coley.recaf.plugin.api.KeybindProvider;
import me.coley.recaf.ui.controls.view.ClassViewport;
import me.coley.recaf.ui.controls.view.FileViewport;
import org.plugface.core.impl.DefaultPluginContext;
import org.plugface.core.internal.AnnotationProcessor;
import org.plugface.core.internal.DependencyResolver;

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
	private final Set<KeybindProvider> keybindProviders = new HashSet<>();
	private final Map<KeybindProvider, Map<ConfKeybinding.Binding, Runnable>>
			globalBinds = new HashMap<>();
	private final Map<KeybindProvider, Map<ConfKeybinding.Binding, Consumer<ClassViewport>>>
			classViewBinds = new HashMap<>();
	private final Map<KeybindProvider, Map<ConfKeybinding.Binding, Consumer<FileViewport>>>
			fileViewBinds = new HashMap<>();

	// Deny constructor
	private PluginKeybinds() {}

	/**
	 * Called by {@link PluginsManager#load()}.
	 */
	void setup() {
		if (keybindProviders.isEmpty()) {
			Collection<KeybindProvider> keybindProviders = PluginsManager.getInstance().ofType(KeybindProvider.class);
			globalBinds.putAll(keybindProviders.stream()
					.collect(Collectors.toMap(provider -> provider, KeybindProvider::createGlobalBindings)));
			classViewBinds.putAll(keybindProviders.stream()
					.collect(Collectors.toMap(provider -> provider, KeybindProvider::createClassViewBindings)));
			fileViewBinds.putAll(keybindProviders.stream()
					.collect(Collectors.toMap(provider -> provider, KeybindProvider::createFileViewBindings)));
		}
	}

	/**
	 * @return Active global binds and their actions.
	 */
	public Map<ConfKeybinding.Binding, Runnable> getGlobalBinds() {
		return globalBinds.entrySet().stream()
				.filter(p -> PluginsManager.getInstance().getPluginStates().containsKey(p.getKey().getName()))
				.flatMap(p -> p.getValue().entrySet().stream())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/**
	 * @return Active class-view binds and their actions.
	 */
	public Map<ConfKeybinding.Binding, Consumer<ClassViewport>> getClassViewBinds() {
		return classViewBinds.entrySet().stream()
				.filter(p -> PluginsManager.getInstance().getPluginStates().containsKey(p.getKey().getName()))
				.flatMap(p -> p.getValue().entrySet().stream())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/**
	 * @return Active file-view binds and their actions.
	 */
	public Map<ConfKeybinding.Binding, Consumer<FileViewport>> getFileViewBinds() {
		return fileViewBinds.entrySet().stream()
				.filter(p -> PluginsManager.getInstance().getPluginStates().containsKey(p.getKey().getName()))
				.flatMap(p -> p.getValue().entrySet().stream())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
