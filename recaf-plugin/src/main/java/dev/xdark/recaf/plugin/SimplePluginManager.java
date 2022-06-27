package dev.xdark.recaf.plugin;

import me.coley.recaf.io.ByteSource;

import java.io.IOException;
import java.util.*;

/**
 * Basic implementation of {@link PluginManager}.
 *
 * @author xDark
 */
public class SimplePluginManager implements PluginManager {
	private final List<PluginLoader> loaders = new ArrayList<>();
	private final Map<String, PluginContainer<?>> nameMap = new HashMap<>();
	private final Map<? super Plugin, PluginContainer<?>> instanceMap = new IdentityHashMap<>();

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
	public Collection<? super PluginContainer<? extends Plugin>> getPlugins() {
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
	public <T extends Plugin> PluginContainer<T> loadPlugin(ByteSource source) throws PluginLoadException {
		for (PluginLoader loader : loaders) {
			try {
				// Skip unsupported sources
				if (!loader.isSupported(source))
					continue;
				// Load and record plugin container
				PluginContainer<T> container = loader.load(source);
				String name = container.getInformation().getName();
				if (nameMap.putIfAbsent(name.toLowerCase(Locale.ROOT), container) != null) {
					// Plugin already exists, we do not allow
					// multiple plugins with the same name.
					try {
						container.getLoader().disablePlugin(container);
					} catch(Exception ignored) {
					}
					throw new PluginLoadException("Duplicate plugin: " + name);
				}
				instanceMap.put(container.getPlugin(), container);
				return container;
			} catch(IOException | UnsupportedSourceException ex) {
				throw new PluginLoadException("Could not load plugin due to an error", ex);
			}
		}
		throw new PluginLoadException("Plugin manager was unable to locate suitable loader for the source.");
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
}
