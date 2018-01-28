package me.coley.recaf.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import me.coley.recaf.Recaf;
import me.coley.recaf.util.Reflect;

public class PluginManager {
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
	private void load(File jar, Set<String> classes) throws IOException {
		// Create loader, and load classes.
		Reflect.extendClasspath(jar);
		for (String name : classes) {
			try {
				Class<?> loaded = Class.forName(name, false, ClassLoader.getSystemClassLoader());
				// Load and register plugins.
				if (Plugin.class.isAssignableFrom(loaded)) {
					Plugin instance = (Plugin) loaded.newInstance();
					loadPlugin(instance);
				}
			} catch (Throwable e) {
				Recaf.INSTANCE.logging.info("From '" + jar.getName() + "' failed to init '" + name + "' because: " + e.toString(), 2);
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
