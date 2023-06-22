package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.plugin.*;
import software.coley.recaf.services.file.RecafDirectoriesConfig;
import software.coley.recaf.util.io.ByteSource;
import software.coley.recaf.util.io.ByteSources;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Basic implementation of {@link PluginManager}.
 *
 * @author xDark
 */
@ApplicationScoped
public class BasicPluginManager implements PluginManager {
	private static final Logger logger = Logging.get(BasicPluginManager.class);
	private final List<PluginLoader> loaders = new ArrayList<>();
	private final Map<String, PluginContainer<?>> nameMap = new HashMap<>();
	private final Map<? super Plugin, PluginContainer<?>> instanceMap = new IdentityHashMap<>();
	private final ClassAllocator classAllocator;
	private final PluginManagerConfig config;

	@Inject
	public BasicPluginManager(PluginManagerConfig config, CdiClassAllocator classAllocator) {
		this.classAllocator = classAllocator;
		this.config = config;

		// Add ZIP/JAR loading
		registerLoader(new ZipPluginLoader(BasicPluginManager.class.getClassLoader()));
	}

	@Inject
	@PostConstruct
	@SuppressWarnings("unused")
	private void setup(RecafDirectoriesConfig directoriesConfig) {
		if (config.isAllowLocalScan())
			scanPlugins(directoriesConfig.getPluginDirectory());
	}

	/**
	 * Scan all plugins in the plugin directory.
	 */
	private void scanPlugins(Path pluginsDir) {
		// Ensure directory exists
		try {
			if (!Files.isDirectory(pluginsDir))
				Files.createDirectories(pluginsDir);

			// Auto close the directory handle
			try (DirectoryStream<Path> paths = Files.newDirectoryStream(pluginsDir)) {
				for (Path pluginPath : paths) {
					// Load all supported plugins
					ByteSource source = ByteSources.forPath(pluginPath);
					if (isSupported(source)) {
						try {
							loadPlugin(source);
						} catch (PluginLoadException ex) {
							logger.error("Failed to load plugin from path: {}", pluginPath, ex);
						}
					}
				}
			}
		} catch (IOException ex) {
			logger.error("Failed to scan local plugin directory '{}'", pluginsDir, ex);
		}
	}

	@Override
	public ClassAllocator getAllocator() {
		return classAllocator;
	}

	@Override
	public PluginLoader getLoader(ByteSource source) throws IOException {
		List<PluginLoader> loaders = this.loaders;
		for (PluginLoader loader : loaders) {
			if (loader.isSupported(source)) {
				return loader;
			}
		}
		return null;
	}

	@Override
	public boolean isSupported(ByteSource source) throws IOException {
		List<PluginLoader> loaders = this.loaders;
		for (PluginLoader loader : loaders) {
			if (loader.isSupported(source)) {
				return true;
			}
		}
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Plugin> PluginContainer<T> getPlugin(String name) {
		return (PluginContainer<T>) nameMap.get(name.toLowerCase(Locale.ROOT));
	}

	@Override
	public Collection<PluginLoader> getLoaders() {
		return Collections.unmodifiableList(loaders);
	}

	@Override
	public Collection<PluginContainer<? extends Plugin>> getPlugins() {
		return Collections.unmodifiableCollection(nameMap.values());
	}

	@Override
	public void registerLoader(PluginLoader loader) {
		loaders.add(loader);
	}

	@Override
	public void removeLoader(PluginLoader loader) {
		loaders.remove(loader);
	}

	@Override
	public <T extends Plugin> PluginContainer<T> loadPlugin(PluginContainer<T> container) throws PluginLoadException {
		String name = container.getInformation().getName();
		if (nameMap.putIfAbsent(name.toLowerCase(Locale.ROOT), container) != null) {
			// Plugin already exists, we do not allow
			// multiple plugins with the same name.
			try {
				container.getLoader().disablePlugin(container);
			} catch (Exception ignored) {
			}
			throw new PluginLoadException("Duplicate plugin: " + name);
		}
		instanceMap.put(container.getPlugin(), container);
		return container;
	}

	@Override
	public void unloadPlugin(String name) {
		PluginContainer<?> container = nameMap.get(name.toLowerCase(Locale.ROOT));
		if (container == null) {
			throw new IllegalStateException("Unknown plugin: " + name);
		}
		unloadPlugin(container);
	}

	@Override
	public void unloadPlugin(Plugin plugin) {
		PluginContainer<?> container = instanceMap.get(plugin);
		if (container == null) {
			throw new IllegalStateException("Unknown plugin: " + plugin);
		}
		unloadPlugin(container);
	}

	@Override
	public void unloadPlugin(PluginContainer<?> container) {
		String name = container.getInformation().getName();
		if (!nameMap.remove(name.toLowerCase(Locale.ROOT), container)
				|| !instanceMap.remove(container.getPlugin(), container)) {
			throw new IllegalStateException("Plugin with name " + name + " does not belong to the container!");
		}
		container.getLoader().disablePlugin(container);
	}

	@Override
	public <T extends Plugin> boolean shouldEnablePluginOnLoad(PluginContainer<T> container) {
		// TODO: We should maintain a state (from config) which plugins the user wants to load on startup
		//  and only allow those to be initialized on startup.
		return true;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public PluginManagerConfig getServiceConfig() {
		return config;
	}
}
