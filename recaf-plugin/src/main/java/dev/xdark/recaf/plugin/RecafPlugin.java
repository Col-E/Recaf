package dev.xdark.recaf.plugin;

import dev.xdark.recaf.plugin.java.ZipPluginLoader;
import me.coley.recaf.io.ByteSources;
import me.coley.recaf.util.Directories;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author xtherk
 */
public class RecafPlugin {
    private static final Logger logger = Logging.get(RecafPlugin.class);
    private final PluginManager pluginManager = new SimplePluginManager();
    private final List<Path> pluginJarUrls = new ArrayList<>();
    private final Map<PluginContainer<? extends Plugin>, Path> pluginContainerPathMap = new HashMap<>();
    private static RecafPlugin INSTANCE;
    private static boolean initialized = false;
    private static final String[] SUFFIX = {".jar"};

    {
        pluginManager.registerLoader(new ZipPluginLoader(RecafPlugin.class.getClassLoader()));
    }

    private RecafPlugin() {
    }

    public static RecafPlugin getInstance() {
        return Objects.requireNonNull(INSTANCE, "Please call the #initialize() first");
    }

    /**
     * Unloads a plugin
     *
     * @param name name of the plugin
     */
    public void unloadPlugin(String name) {
        getPlugin(name).ifPresent(pc -> {
            pluginManager.unloadPlugin(name);
            pluginJarUrls.remove(pluginContainerPathMap.remove(pc));
        });
    }

    /**
     * Init plugin information
     */
    public static void initialize() {
        if (!initialized) {
            INSTANCE = new RecafPlugin();
            try {
                INSTANCE.scanningPlugin();
                INSTANCE.loadPlugins();
            } catch (IOException ex) {
                logger.error("Please check the plugin directory", ex);
            }
            initialized = true;
        }
    }

    /**
     * @param path The path where the plugin is located
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
     * @param plugins The plugin name set that needs to be activated
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
        if (pluginJarUrls.isEmpty()) return;
        Iterator<Path> entryIterator = pluginJarUrls.iterator();
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
     * Scan all plugins in the plugin directory
     *
     * @throws IOException When the plugin folder cannot be created.
     */
    private void scanningPlugin() throws IOException {
        Path pluginsDir = Directories.getPluginDirectory();
        // Ensure directory exists
        if (!Files.isDirectory(pluginsDir))
            Files.createDirectories(pluginsDir);

        for (Path filePath : Files.newDirectoryStream(pluginsDir)) {
            // Maybe we should support ".zip"
            for (String suffix : SUFFIX) {
                if (filePath.getFileName().toString().endsWith(suffix)) {
                    pluginJarUrls.add(filePath);
                }
            }
        }
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public Optional<PluginContainer<Plugin>> getPlugin(String name) {
        return Optional.ofNullable(pluginManager.getPlugin(name));
    }

    public Map<PluginContainer<? extends Plugin>, Path> getPluginContainerPathMap() {
        return Collections.unmodifiableMap(pluginContainerPathMap);
    }
}
