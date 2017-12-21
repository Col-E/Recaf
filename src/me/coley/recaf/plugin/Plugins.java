package me.coley.recaf.plugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import me.coley.recaf.Recaf;

public class Plugins {
	private final File pluginsDirectory = new File("plugins" + File.separator);
	private final Map<String, Plugin> plugins = new HashMap<>();

	/**
	 * Load plugins from directory.
	 */
	public void init() {
		// Create directory if it does not exist.
		if (!pluginsDirectory.exists()) {
			pluginsDirectory.mkdirs();
		}
		// Iterate plugins in directory
		for (File file : pluginsDirectory.listFiles()) {
			if (file.getName().endsWith(".jar")) {
				try (ZipFile zip = new ZipFile(file)) {
					// Set of classes in jar.
					Set<String> classes = new HashSet<>();
					// Find classes in the jar
					Enumeration<? extends ZipEntry> entries = zip.entries();
					while (entries.hasMoreElements()) {
						ZipEntry entry = entries.nextElement();
						if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
							continue;
						}
						classes.add(entry.getName().replace(".class", "").replace("/", "."));
					}
					// No more entries, load from jar with known classes.
					load(file, classes);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Initialize any plugins in the given jar.
	 * 
	 * @param jar
	 *            Jar containing plugin classes.
	 * @param classes
	 *            Set of all classes within the jar.
	 * @throws Exception
	 *             Thrown if URLClassLoader couldn't be made or an instance of a
	 *             class could not be loaded.
	 */
	@SuppressWarnings("deprecation")
	private void load(File jar, Set<String> classes) throws Exception {
		// Create loader, and load classes.
		URLClassLoader child = new URLClassLoader(new URL[] { jar.toURL() }, ClassLoader.getSystemClassLoader());
		for (String name : classes) {
			Class<?> loaded = Class.forName(name, true, child);
			// Load and register plugins.
			if (Plugin.class.isAssignableFrom(loaded)) {
				Plugin instance = (Plugin) loaded.newInstance();
				loadPlugin(instance);
			}
		}
	}

	/**
	 * Loads the plugin and registers it for event listening.
	 * 
	 * @param instance
	 *            The plugin.
	 */
	private void loadPlugin(Plugin instance) {
		instance.init();
		Recaf.INSTANCE.bus.subscribe(instance);
		plugins.put(instance.getID(), instance);
	}

	/**
	 * @return Map of plugin ID's to plugins.
	 */
	public Map<String, Plugin> getPlugins() {
		return this.plugins;
	}

}
