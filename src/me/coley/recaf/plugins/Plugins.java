package me.coley.recaf.plugins;

import java.io.File;
import java.util.Collection;
import java.util.function.Predicate;

import me.coley.plugin.LoadStrategy;
import me.coley.plugin.Plugin;
import me.coley.plugin.PluginManager;
import me.coley.plugin.PluginManagerFactory;
import me.coley.plugin.impl.dir.DirectoryLoadingStrategy;

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
	 * @param filter
	 *            Match filter.
	 * @return Collection that pass the filter.
	 */
	public static Collection<Plugin> get(Predicate<Plugin> filter) {
		return INSTANCE.getPlugins(filter);
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
	}
	//@formatter:on
}
