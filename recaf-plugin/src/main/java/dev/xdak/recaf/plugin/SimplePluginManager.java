package dev.xdak.recaf.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Basic implementation of {@link PluginManager}.
 *
 * @author xDark
 */
public final class SimplePluginManager implements PluginManager {

    private final List<PluginLoader> loaders = new ArrayList<>();
    private final Map<String, PluginContainer<?>> nameMap = new HashMap<>();
    private final Map<? super Plugin, PluginContainer<?>> instanceMap = new IdentityHashMap<>();

    @Override
    public PluginLoader getLoader(InputStream in) throws IOException {
        List<PluginLoader> loaders = this.loaders;
        for (PluginLoader loader : loaders) {
            if (loader.isSupported(in)) {
                return loader;
            }
            in.reset();
        }
        return null;
    }

    @Override
    public <T extends Plugin> PluginContainer<T> getPlugin(String name) {
        return (PluginContainer<T>) nameMap.get(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public Collection<PluginLoader> getLoaders() {
        return Collections.unmodifiableList(loaders);
    }

    @Override
    public Collection<? super Plugin> getPlugins() {
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
    public <T extends Plugin> PluginContainer<T> loadPlugin(InputStream in) throws PluginLoadException {
        for (PluginLoader loader : loaders) {
            try {
                PluginContainer<T> container = loader.load(in);

                String name = container.getInformation().getName();
                if (nameMap.putIfAbsent(name.toLowerCase(Locale.ROOT), container) != null) {
                    // Plugin already exists, we do not allow
                    // multiple plugins with the same name.
                    // TODO logging
                    try {
                        container.getLoader().disablePlugin(container.getPlugin());
                    } catch (Exception ignored) {
                    }
                    throw new PluginLoadException("Duplicate plugin: " + name);
                }
                instanceMap.put(container.getPlugin(), container);
                return container;
            } catch (IOException | UnsupportedSourceException e) {
                try {
                    in.reset();
                } catch (IOException ex) {
                    throw new PluginLoadException("Could not reset input stream", ex);
                }
            }
        }
        throw new PluginLoadException("Plugin manager was unable to locate suitable loader for the source.");
    }

    @Override
    public void unloadPlugin(String plugin) {
        PluginContainer<?> container = nameMap.get(plugin.toLowerCase(Locale.ROOT));
        if (container == null) {
            throw new IllegalStateException("Unknown plugin: " + plugin);
        }
        unloadPlugin(container);
    }

    @Override
    public void unloadPlugin(Object plugin) {
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
        container.getLoader().disablePlugin(container.getPlugin());
    }
}
