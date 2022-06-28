package dev.xdark.recaf.plugin;

import dev.xdark.recaf.plugin.java.ZipPluginLoader;
import me.coley.recaf.io.ByteSource;
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
 * {@link PluginManager} that reads from {@link Directories#getPluginDirectory()}.
 *
 * @author xtherk
 * @author Matt Coley
 */
public class RecafPluginManager extends SimplePluginManager {
	private static final Logger logger = Logging.get(RecafPluginManager.class);
	private final Map<PluginContainer<? extends Plugin>, Path> pluginContainerPathMap = new HashMap<>();
	private static RecafPluginManager INSTANCE;
	private static boolean initialized = false;

	private RecafPluginManager() {
		registerLoader(new ZipPluginLoader(RecafPluginManager.class.getClassLoader()));
	}

	@Override
	public void unloadPlugin(String name) {
		optionalGetPlugin(name).ifPresent(pc -> {
			super.unloadPlugin(name);
			pluginContainerPathMap.remove(pc);
		});
	}

	/**
	 * @return Root instance.
	 */
	public static RecafPluginManager getInstance() {
		return Objects.requireNonNull(INSTANCE, "Please call the #initialize() first");
	}

	/**
	 * @param path
	 * 		The path where the plugin is located
	 *
	 * @return Return to PluginContainer after loading
	 */
	public Optional<PluginContainer<Plugin>> loadPlugin(Path path) {
		try {
			PluginContainer<Plugin> pc = loadPlugin(ByteSources.forPath(path));
			pluginContainerPathMap.put(pc, path);
			return Optional.of(pc);
		} catch (PluginLoadException ex) {
			logger.error("Failed to load plugin from path: {}", path, ex);
		}
		return Optional.empty();
	}

	/**
	 * @param plugins
	 * 		The plugin name set that needs to be activated
	 */
	public void enablePlugins(Set<String> plugins) {
		plugins.forEach(name -> optionalGetPlugin(name)
				.ifPresent(plugin -> plugin.getLoader().enablePlugin(plugin))
		);
	}

	/**
	 * @param name
	 * 		Name of the plugin to get.
	 *
	 * @return Instance of the plugin container, or {@code null} if no plugin by the name exists.
	 */
	public Optional<PluginContainer<Plugin>> optionalGetPlugin(String name) {
		return Optional.ofNullable(getPlugin(name));
	}

	/**
	 * @return Map of plugins to the file paths they are loaded from.
	 */
	public Map<PluginContainer<? extends Plugin>, Path> getPluginContainerPathMap() {
		return Collections.unmodifiableMap(pluginContainerPathMap);
	}

	/**
	 * Initialize {@link RecafPluginManager} instance.
	 */
	public static void initialize() {
		if (!initialized) {
			INSTANCE = new RecafPluginManager();
			try {
				List<Path> paths = INSTANCE.scanPlugins();
				for (Path path : paths)
					INSTANCE.loadPlugin(path);
			} catch (IOException ex) {
				logger.error("Please check the plugin directory", ex);
			}
			initialized = true;
		}
	}

	/**
	 * Scan all plugins in the plugin directory.
	 *
	 * @return List of plugin paths.
	 *
	 * @throws IOException
	 * 		When the plugin folder cannot be created.
	 */
	private List<Path> scanPlugins() throws IOException {
		List<Path> pluginJarPaths = new ArrayList<>();
		Path pluginsDir = Directories.getPluginDirectory();
		// Ensure directory exists
		if (!Files.isDirectory(pluginsDir))
			Files.createDirectories(pluginsDir);
		// Auto close the directory handle
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(pluginsDir)) {
			for (Path pluginPath : paths) {
				ByteSource source = ByteSources.forPath(pluginPath);
				if (isSupported(source)) {
					pluginJarPaths.add(pluginPath);
				}
			}
		}
		return pluginJarPaths;
	}
}
