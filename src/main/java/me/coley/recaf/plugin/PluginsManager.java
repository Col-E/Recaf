package me.coley.recaf.plugin;

import me.coley.recaf.plugin.api.BasePlugin;
import me.coley.recaf.plugin.api.InternalPlugin;
import me.coley.recaf.plugin.api.InternalApi;
import me.coley.recaf.util.Log;
import me.coley.recaf.workspace.EntryLoader;
import org.plugface.core.PluginContext;
import org.plugface.core.impl.DefaultPluginContext;
import org.plugface.core.impl.DefaultPluginManager;
import org.plugface.core.internal.AnnotationProcessor;
import org.plugface.core.internal.DependencyResolver;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Plugin manager.
 *
 * @author Matt
 */
public class PluginsManager extends DefaultPluginManager {
	private static final PluginsManager INSTANCE;
	private final Map<String, BasePlugin> plugins = new HashMap<>();
	private final Map<String, Boolean> pluginStates = new HashMap<>();
	private final Map<String, BufferedImage> pluginIcons = new HashMap<>();
	private EntryLoader entryLoader;

	/**
	 * Deny public construction.
	 */
	private PluginsManager(PluginContext context, AnnotationProcessor processor,
						   DependencyResolver resolver) {
		super(context, processor, resolver);
	}

	/**
	 * Initialize the plugin manager.
	 *
	 * @throws Exception
	 * 		See other throws.
	 * @throws PluginLoadException
	 * 		When loading specific plugin crashes when loaded.
	 * @throws IOException
	 * 		When the plugin directory cannot be read from or created.
	 * @throws IllegalStateException
	 * 		When the plugin does not properly define a name.
	 */
	public void load() throws Exception {
		PluginFolderSource source = new PluginFolderSource();
		// Collect plugin instances
		Collection<Object> instances = loadPlugins(source);
		for (Object instance : instances) {
			if (instance instanceof BasePlugin) {
				BasePlugin plugin = (BasePlugin) instance;
				String name = plugin.getName();
				String version = plugin.getVersion();
				String className = instance.getClass().getName();
				BufferedImage icon =
						source.getPluginIcons().get(source.getClassToPlugin().get(className));
				Log.info("Discovered plugin '{}-{}'", name, version);
				// PlugFace already has its own internal storage of the plugin instances,
				// but we want to control them a bit easier. So we'll keep a local reference.
				addPlugin(name, plugin, icon);
			} else {
				Log.error("Class '{}' does not extend plugin!", instance.getClass().getName());
			}
		}
		// Done
		if (!plugins.isEmpty())
			Log.info("Loaded {} plugins", plugins.size());
	}


	/**
	 * @return Collection of all plugin instances.
	 */
	public Map<String, BasePlugin> plugins() {
		return plugins;
	}

	/**
	 * @return Collection of visible plugin instances.
	 */
	public Map<String, BasePlugin> visiblePlugins() {
		return plugins().entrySet()
				.stream()
				.filter(e -> !(e.getValue() instanceof InternalPlugin))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/**
	 * @return Map of plugin states.
	 */
	public Map<String, Boolean> getPluginStates() {
		return pluginStates;
	}

	/**
	 * Map of plugin's icons. Not all plugins have icons.
	 *
	 * @return Map of plugin icons.
	 */
	public Map<String, BufferedImage> getPluginIcons() {
		return pluginIcons;
	}

	/**
	 * @return {@code true} when 1 or more plugins have been loaded.
	 */
	public boolean hasPlugins() {
		return !plugins.isEmpty();
	}

	/**
	 * @return The current entry loader defined by a plugin.
	 */
	public EntryLoader getEntryLoader() {
		return entryLoader;
	}

	/**
	 * Set current entry loader.
	 *
	 * @param entryLoader
	 * 		New entry loader defined by a plugin.
	 */
	public void setEntryLoader(EntryLoader entryLoader) {
		this.entryLoader = entryLoader;
	}

	/**
	 * Fetch the active plugins matching the given type.
	 * This will exclude plugins that are disabled.
	 *
	 * @param type
	 * 		Class of plugin.
	 * @param <T>
	 * 		Plugin type.
	 *
	 * @return Collection of active plugins matching the given type.
	 */
	@SuppressWarnings("unchecked")
	public <T extends BasePlugin> Collection<T> ofType(Class<T> type) {
		return plugins().values().stream()
						.filter(plugin -> type.isAssignableFrom(plugin.getClass()))
						.filter(plugin -> pluginStates.getOrDefault(plugin.getName(), false))
						.map(plugin -> (T) plugin)
						.collect(Collectors.toList());
	}

	/**
	 * @return Plugins manager instance.
	 */
	public static PluginsManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Registers a plugin.
	 *
	 * @param name
	 * 		Name of the plugin.
	 * @param plugin
	 * 		Plugin to register.
	 * @param icon
	 * 		Icon of the plugin.
	 */
	@InternalApi
	public void addPlugin(String name, BasePlugin plugin, BufferedImage icon) {
		plugins.put(name, plugin);
		pluginStates.put(name, Boolean.TRUE);
		if (icon != null) {
			pluginIcons.put(name, icon);
		}
	}

	/**
	 * Registers a plugin.
	 *
	 * @param plugin
	 * 		Plugin to register.
	 * @param icon
	 * 		Icon of the plugin.
	 */
	@InternalApi
	public void addPlugin(BasePlugin plugin, BufferedImage icon) {
		addPlugin(plugin.getName(), plugin, icon);
	}

	/**
	 * Registers a plugin.
	 *
	 * @param plugin
	 * 		Plugin to register.
	 */
	@InternalApi
	public void addPlugin(BasePlugin plugin) {
		addPlugin(plugin, null);
	}

	static {
		DefaultPluginContext context = new DefaultPluginContext();
		AnnotationProcessor processor = new AnnotationProcessor();
		DependencyResolver resolver = new DependencyResolver(processor);
		INSTANCE = new PluginsManager(context, processor, resolver);
	}
}
