package me.coley.recaf.plugin;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import org.plugface.core.PluginManager;
import org.plugface.core.factory.PluginManagers;
import org.plugface.core.factory.PluginSources;

import me.coley.recaf.Logging;

/**
 * Plugin manager.
 * 
 * @author Matt
 */
public class Plugins {
	private static final File PLUGIN_DIR = new File("plugins");
	private static final Plugins INSTANCE;
	private final PluginManager manager = PluginManagers.defaultPluginManager();
	private Collection<PluginBase> plugins;

	private Plugins() {
		initialize();
	}

	/**
	 * Setup plugins collection.
	 */
	private void initialize() {
		try {
			Collection<Object> objects = manager.loadPlugins(PluginSources.jarSource(PLUGIN_DIR.toURI()));
			plugins = objects.stream().map(o -> (PluginBase) o).collect(Collectors.toSet());
			if (plugins.size() > 0)
				Logging.info("Loaded " + plugins.size() + " plugins");
			for (PluginBase p : plugins)
				Logging.info(String.format("Plugin %s %s", p.name(), p.version()), 1);
			for (PluginBase p : plugins)
				try {
					p.onLoad();
				} catch (Exception e) {
					Logging.error(String.format("Plugin '%s' threw an exception in 'onLoad'", p.name()));
					Logging.error(e);
				}
		} catch (Throwable e) {
			Logging.error("Failed to load plugins from 'plugins' directory.");
			Logging.error(e, false);
			plugins = Collections.emptySet();
		}
	}

	/**
	 * @return Collection of all plugins.
	 */
	public Collection<PluginBase> plugins() {
		return plugins;
	}

	/**
	 * @param type
	 *            Class of plugin.
	 * @return Collection of plugins matching the given type.
	 */
	@SuppressWarnings("unchecked")
	public <T> Collection<T> plugins(Class<T> type) {
		//@formatter:off
		return plugins().stream()
				.filter(plugin -> type.isAssignableFrom(plugin.getClass()))
				.map(plugin -> (T) plugin)
				.collect(Collectors.toList());
		//@formatter:on
	}

	/**
	 * Retrieves the first plugin of the given type from
	 * {@link #plugins(Class)}. If no such plugin exists {@code null} is
	 * returned.
	 * 
	 * @param type
	 *            Class of plugin.
	 * @return Plugin instance of class. May be null if no plugin of the type
	 *         exists.
	 */
	public <T> T plugin(Class<T> type) {
		Optional<T> opt = plugins(type).stream().findFirst();
		return opt.orElse(null);
	}

	/**
	 * @return Directory that contains plugins.
	 */
	public File getDirectory() {
		return PLUGIN_DIR;
	}

	/**
	 * @return Plugin manager instance.
	 */
	public static Plugins instance() {
		return INSTANCE;
	}

	static {
		// create directory if it does not exist
		if (!PLUGIN_DIR.exists()) {
			PLUGIN_DIR.mkdir();
		}
		// initialize the instance
		INSTANCE = new Plugins();
	}
}
