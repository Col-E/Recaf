package me.coley.recaf.plugin;

import me.coley.recaf.Recaf;
import me.coley.recaf.plugin.api.BasePlugin;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.LangUtil;
import me.coley.recaf.util.VMUtil;
import org.objectweb.asm.ClassReader;
import org.plugface.core.PluginSource;
import org.plugface.core.internal.PluginClassLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A plugin source that loads from Recaf's plugins directory.
 *
 * @author Matt
 */
public class PluginFolderSource implements PluginSource {
	private final Map<Path, URL> pluginJarUrls = new HashMap<>();
	private final Map<Path, BufferedImage> pluginIcons = new HashMap<>();
	private final Map<String, Path> classToPlugin = new HashMap<>();

	/**
	 * Create the plugin folder source.
	 *
	 * @throws IOException
	 * 		When the plugin folder cannot be found or generated.
	 */
	public 	PluginFolderSource() throws IOException {
		Path pluginsDir = getPluginDir();
		// Ensure directory exists
		if(!Files.isDirectory(pluginsDir))
			Files.createDirectories(pluginsDir);
		// Populate plugin map
		for(Path filePath : Files.newDirectoryStream(pluginsDir)) {
			if(filePath.getFileName().toString().endsWith(".jar")) {
				pluginJarUrls.put(filePath, filePath.toUri().toURL());
			}
		}
	}

	@Override
	public Collection<Class<?>> load() throws PluginLoadException {
		List<Class<?>> plugins = new ArrayList<>();
		PluginClassLoader loader =
				new PluginClassLoader(pluginJarUrls.values().toArray(new URL[0]));
		VMUtil.setParent(loader, Recaf.class.getClassLoader());
		for(Path pluginPath : pluginJarUrls.keySet()) {
			File path = pluginPath.toAbsolutePath().toFile();
			String className = null;
			try (JarFile jar = new JarFile(path)) {
				for(Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
					JarEntry entry = entries.nextElement();
					if(entry.isDirectory())
						continue;
					// Add classes
					if (entry.getName().endsWith(".class")) {
						className = toName(entry);
						if (isPluginClass(jar, entry)) {
							plugins.add(Class.forName(className, false, loader));
						}
						classToPlugin.put(className, pluginPath);
					}
					// Check for plugin icon
					else if(entry.getName().endsWith("icon.png")) {
						BufferedImage image = ImageIO.read(jar.getInputStream(entry));
						pluginIcons.put(pluginPath, image);
					}
					// Check for translation files
					else if(entry.getName().endsWith(LangUtil.DEFAULT_LANGUAGE + ".json")){
						LangUtil.load(jar.getInputStream(entry));
					}
				}
			} catch(IOException ex) {
				throw new PluginLoadException(path, ex, "Failed to load jar file");
			} catch(ReflectiveOperationException ex) {
				throw new PluginLoadException(path, ex, "Failed to load '" + className + "' in jar");
			}
		}
		return plugins;
	}

	/**
	 * @param jar
	 * 		Container.
	 * @param entry
	 * 		Entry in container that has the class file extension.
	 *
	 * @return {@code true} if the entry is a class that implements a plugin API interface.
	 *
	 * @throws IOException
	 * 		When the entry cannot be read.
	 */
	private boolean isPluginClass(JarFile jar, JarEntry entry) throws IOException {
		byte[] content = IOUtil.toByteArray(jar.getInputStream(entry));
		String[] interfaces = new ClassReader(content).getInterfaces();
		String pluginPackage = BasePlugin.class.getPackage().getName().replace('.', '/');
		for (String itf : interfaces) {
			if (itf.startsWith(pluginPackage))
				return true;
		}
		return false;
	}

	/**
	 * @return Map of plugin paths to their icons. Not all plugins have icons.
	 */
	public Map<Path, BufferedImage> getPluginIcons() {
		return pluginIcons;
	}

	/**
	 * @return Map of plugin class names <i>(qualified names)</i> to their containing plugin-jar's path.
	 */
	public Map<String, Path> getClassToPlugin() {
		return classToPlugin;
	}

	/**
	 * Entry to qualified class name.
	 *
	 * @param entry
	 * 		Jar entry of a class file.
	 *
	 * @return Class name.
	 */
	private static String toName(JarEntry entry) {
		return entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
	}

	/**
	 * @return Directory for Recaf plugins.
	 */
	private static Path getPluginDir() {
		return Recaf.getDirectory("plugins");
	}
}
