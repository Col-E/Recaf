package me.coley.recaf.plugins;

import java.io.File;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import me.coley.plugin.LoadStrategy;
import me.coley.plugin.Plugin;
import me.coley.plugin.PluginConfig;
import me.coley.plugin.PluginManager;
import me.coley.plugin.PluginManagerFactory;
import me.coley.plugin.impl.dir.DirectoryLoadingStrategy;
import me.coley.plugin.impl.dir.InvalidPluginDirectoryException;
import me.coley.recaf.Logging;

/**
 * Plugin manager.
 * 
 * @author Matt
 */
public class Plugins extends PluginManager {
	private static final File PLUGIN_DIR = new File("plugins");
	private static final Plugins INSTANCE;

	public Plugins(LoadStrategy strategy, File directory) {
		super(strategy, directory);
	}

	/**
	 * @return Collection of all plugins.
	 */
	public static Collection<Plugin> get() {
		return INSTANCE.getPlugins();
	}

	/**
	 * Returns the collection of Stageable plugins that are shown in the plugins
	 * menu.
	 * 
	 * @return Collection of Stageable plugins.
	 */
	public static Collection<Stageable> getStageables() {
		//@formatter:off
		return get(plugin -> plugin instanceof Stageable)
				.stream()
				.map(pl -> (Stageable) pl)
				.collect(Collectors.toList());
		//@formatter:on
	}

	/**
	 * Returns the collection of Launchable plugins that are called when Recaf
	 * starts.
	 * 
	 * @return Collection of Launchable plugins.
	 */
	public static Collection<Launchable> getLaunchables() {
		//@formatter:off
		return get(plugin -> plugin instanceof Launchable)
				.stream()
				.map(pl -> (Launchable) pl)
				.collect(Collectors.toList());
		//@formatter:on
	}

	/**
	 * @param filter
	 *            Match filter.
	 * @return Collection that pass the filter.
	 */
	public static Collection<Plugin> get(Predicate<Plugin> filter) {
		return INSTANCE.getPlugins(filter);
	}

	@Override
	public void onPluginInitFail(File directory, PluginConfig config) {
		Logging.error("Plugin '" + directory.getName() + "' failed to initialize.");
	}

	@Override
	public void onPluginConfigFail(File directory, File config) {
		Logging.error("Plugin config '" + directory.getName() + "/" + config.getName() + "' is malformed.");
	}

	@Override
	public void onPluginPopulateError(File directory, Exception e) {
		InvalidPluginDirectoryException i = (InvalidPluginDirectoryException) e;
		String reason = "?";
		switch (i.getReason()) {
		case BAD_DIR_INVALID_URL:
			reason = "Plugin directory is malformed";
			break;
		case BAD_DIR_NOPLUGIN:
			reason = "Plugin directory is missing 'plugin-{VERSION}.jar'";
			break;
		case BAD_DIR_NOSETTINGS:
			reason = "Plugin directory is missing 'settings.json'";
			break;
		case IO_BAD_SETTINGS:
			reason = "Plugin 'settings.json' is malformed";
			break;
		case PLUGIN_FAILED_TO_LOAD:
			reason = "Plugin failed to initalize";
			break;
		}
		Logging.error("Plugin directory '" + directory.getName() + "' is failed because: " + reason);
	}

	/**
	 * Close the plugin manager, unload all plugins.
	 */
	public static void close() {
		INSTANCE.shutdown();
	}

	/**
	 * @return Directory containing plugin folders.
	 */
	public static File getPluginDirectory() {
		return PLUGIN_DIR;
	}

	//@formatter:off
	static {
		
		// create directory if it does not exist
		if (!PLUGIN_DIR.exists()) {
			PLUGIN_DIR.mkdir();
		}
		// initialize the instance
		INSTANCE = (Plugins) PluginManagerFactory.create(
			new DirectoryLoadingStrategy(), 
			PLUGIN_DIR, 
			(strategy, directory) -> {
				return new Plugins(strategy, directory);
			});
		// load the plugins
		INSTANCE.loadPlugins();
	}
	//@formatter:on
}
