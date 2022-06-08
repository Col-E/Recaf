package dev.xdark.recaf.plugin;

import dev.xdark.recaf.plugin.java.ZipPluginLoader;
import me.coley.recaf.io.ByteSources;
import me.coley.recaf.util.Directories;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Root plugin for Recaf, providing access to a {@link PluginManager} that reads from
 * {@link Directories#getPluginDirectory()}.
 *
 * @author xtherk
 */
public class RecafRootPlugin {
	private static final Logger logger = Logging.get(RecafRootPlugin.class);
	private final PluginManager pluginManager = new SimplePluginManager();
	private final List<Path> pluginJarPaths = new ArrayList<>();
	private final Map<PluginContainer<? extends Plugin>, Path> pluginContainerPathMap = new HashMap<>();
	private static RecafRootPlugin INSTANCE;
	private static boolean initialized = false;
	private static final String[] SUFFIX = {".jar"};

	private RecafRootPlugin() {
		pluginManager.registerLoader(new ZipPluginLoader(RecafRootPlugin.class.getClassLoader()));
	}

	/**
	 * @return Root instance.
	 */
	public static RecafRootPlugin getInstance() {
		return Objects.requireNonNull(INSTANCE, "Please call the #initialize() first");
	}

	/**
	 * Unloads a plugin.
	 *
	 * @param name
	 * 		Name of the plugin.
	 */
	public void unloadPlugin(String name) {
		getPlugin(name).ifPresent(pc -> {
			pluginManager.unloadPlugin(name);
			pluginJarPaths.remove(pluginContainerPathMap.remove(pc));
		});
	}

	/**
	 * Initialize {@link RecafRootPlugin} instance.
	 */
	public static void initialize() {
		if (!initialized) {
			INSTANCE = new RecafRootPlugin();
			try {
				INSTANCE.scanPlugins();
				INSTANCE.loadPlugins();
			} catch (IOException ex) {
				logger.error("Please check the plugin directory", ex);
			}
			initialized = true;
		}
	}

	/**
	 * @param path
	 * 		The path where the plugin is located
	 *
	 * @return Return to PluginContainer after loading
	 */
	public Optional<PluginContainer<Plugin>> loadPlugin(Path path) {
		try {
			PluginContainer<Plugin> pc = pluginManager.loadPlugin(ByteSources.forPath(path));
			pluginContainerPathMap.put(pc, path);
			return Optional.of(pc);
		} catch (PluginLoadException ex) {
			logger.error("Failed to load the plugin", ex);
		}
		return Optional.empty();
	}

	/**
	 * @param plugins
	 * 		The plugin name set that needs to be activated
	 */
	public void enablePlugins(Set<String> plugins) {
		plugins.forEach(name -> getPlugin(name)
				.ifPresent(plugin -> plugin.getLoader().enablePlugin(plugin))
		);
	}

	/**
	 * Load all plugins into memory.
	 */
	private void loadPlugins() {
		if (pluginJarPaths.isEmpty()) return;
		Iterator<Path> entryIterator = pluginJarPaths.iterator();
		while (entryIterator.hasNext()) {
			Path path = entryIterator.next();
			try {
				PluginContainer<Plugin> pc = pluginManager.loadPlugin(ByteSources.forPath(path));
				pluginContainerPathMap.put(pc, path);
			} catch (PluginLoadException ex) {
				logger.error("Failed to load the plugin", ex);
				entryIterator.remove();
			}
		}
	}

	/**
	 * Scan all plugins in the plugin directory.
	 *
	 * @throws IOException
	 * 		When the plugin folder cannot be created.
	 */
	private void scanPlugins() throws IOException {
		Path pluginsDir = Directories.getPluginDirectory();
		// Ensure directory exists
		if (!Files.isDirectory(pluginsDir))
			Files.createDirectories(pluginsDir);

		// Auto close the directory handle
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(pluginsDir)) {
			for (Path pluginPath : paths) {
				// Maybe we should support ".zip"
				for (String suffix : SUFFIX) {
					if (pluginPath.getFileName().toString().endsWith(suffix)) {
						pluginJarPaths.add(pluginPath);
					}
				}
			}
		}
	}

	/**
	 * @return Plugin manager instance.
	 */
	public PluginManager getPluginManager() {
		return pluginManager;
	}

	/**
	 * @param name
	 * 		Name of the plugin to get.
	 *
	 * @return Instance of the plugin container, or {@code null} if no plugin by the name exists.
	 */
	public Optional<PluginContainer<Plugin>> getPlugin(String name) {
		return Optional.ofNullable(pluginManager.getPlugin(name));
	}

	/**
	 * @return Map of plugins to the file paths they are loaded from.
	 */
	public Map<PluginContainer<? extends Plugin>, Path> getPluginContainerPathMap() {
		return Collections.unmodifiableMap(pluginContainerPathMap);
	}
}
