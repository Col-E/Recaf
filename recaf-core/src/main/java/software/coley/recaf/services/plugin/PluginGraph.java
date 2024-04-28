package software.coley.recaf.services.plugin;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.plugin.ClassAllocator;
import software.coley.recaf.plugin.Plugin;
import software.coley.recaf.plugin.PluginContainer;
import software.coley.recaf.plugin.PluginException;
import software.coley.recaf.plugin.PluginInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

final class PluginGraph {
	final Map<String, LoadedPlugin> plugins = HashMap.newHashMap(16);
	private final ClassAllocator classAllocator;

	PluginGraph(ClassAllocator classAllocator) {
		this.classAllocator = classAllocator;
	}

	@Nonnull
	Collection<PluginContainer<?>> apply(@Nonnull List<PreparedPlugin> preparedPlugins) throws PluginException {
		Map<String, LoadedPlugin> temp = LinkedHashMap.newLinkedHashMap(preparedPlugins.size());
		var plugins = this.plugins;
		for (var preparedPlugin : preparedPlugins) {
			String id = preparedPlugin.info().id();
			if (plugins.containsKey(id)) {
				throw new PluginException("Plugin %s is already loaded".formatted(id));
			}
			var classLoader = new PluginClassLoaderImpl(this, preparedPlugin.pluginSource(), id);
			LoadedPlugin loadedPlugin = new LoadedPlugin(new PluginContainerImpl<>(preparedPlugin, classLoader));

			if (temp.putIfAbsent(id, loadedPlugin) != null) {
				throw new PluginException("Duplicate plugin %s".formatted(id));
			}
		}
		for (LoadedPlugin plugin : temp.values()) {
			PluginInfo info = plugin.container.info();
			for (String dependencyId : info.dependencies()) {
				LoadedPlugin dep = temp.get(dependencyId);
				if (dep == null) {
					dep = plugins.get(dependencyId);
				}
				if (dep == null) {
					throw new PluginException("Plugin %s is missing dependency %s".formatted(info.id(), dependencyId));
				}
				plugin.dependencies.add(dep);
			}
			for (String dependencyId : info.softDependencies()) {
				LoadedPlugin dep = temp.get(dependencyId);
				if (dep == null && (dep = plugins.get(dependencyId)) == null) {
					continue;
				}
				plugin.dependencies.add(dep);
			}
		}
		for (LoadedPlugin loadedPlugin : temp.values()) {
			try {
				enable(loadedPlugin);
			} catch (PluginException ex) {
				for (LoadedPlugin pl : temp.values()) {
					PluginContainerImpl<?> container = pl.container;
					try {
						try {
							Plugin maybeEnabled = container.plugin;
							if (maybeEnabled != null) {
								maybeEnabled.onDisable();
							}
						} finally {
							container.preparedPlugin.reject();
						}
					} catch (Exception ex1) {
						ex.addSuppressed(ex1);
					}
				}
				throw ex;
			}
		}
		plugins.putAll(temp);
		return Collections2.transform(temp.values(), input -> input.container);
	}

	@Nonnull
	PluginUnloader unload(@Nonnull String id) {
		LoadedPlugin plugin = plugins.get(id);
		if (plugin == null) {
			throw new IllegalStateException("Plugin %s is not loaded".formatted(id));
		}
		Map<LoadedPlugin, Set<LoadedPlugin>> dependants = HashMap.newHashMap(8);
		collectDependants(plugin, dependants);
		return new PluginUnloader() {
			@Override
			public void commit() throws PluginException {
				PluginException ex = unload(plugin, dependants);
				if (ex != null)
					throw ex;
			}

			@Nonnull
			@Override
			public PluginInfo unloadingPlugin() {
				return plugin.container.info();
			}

			@Nonnull
			@Override
			public Stream<PluginInfo> dependants() {
				return dependants.values()
						.stream()
						.flatMap(Collection::stream)
						.map(plugin -> plugin.container.info());
			}
		};
	}

	private PluginException unload(LoadedPlugin plugin, Map<LoadedPlugin, Set<LoadedPlugin>> dependants) {
		String id = plugin.container.info().id();
		if (!plugins.remove(id, plugin)) {
			throw new IllegalStateException("Plugin %s was already removed, recursion?".formatted(id));
		}
		PluginException exception = null;
		for (LoadedPlugin dependant : dependants.get(plugin)) {
			PluginException inner = unload(dependant, dependants);
			if (inner != null) {
				if (exception == null) {
					exception = inner;
				} else  {
					exception.addSuppressed(inner);
				}
			}
		}
		try {
			PluginContainerImpl<?> container = plugin.container;
			try {
				container.plugin().onDisable();
			} finally {
				if (container.classLoader instanceof AutoCloseable ac) {
					ac.close();
				}
			}
		} catch (Exception ex) {
			PluginException pex = new PluginException(ex);
			if (exception == null) {
				exception = pex;
			} else {
				exception.addSuppressed(pex);
			}
		}
		return exception;
	}

	private void collectDependants(LoadedPlugin plugin, Map<LoadedPlugin, Set<LoadedPlugin>> dependants) {
		Set<LoadedPlugin> dependantsSet = dependants.computeIfAbsent(plugin, __ -> HashSet.newHashSet(4));
		for (LoadedPlugin pl : plugins.values()) {
			if (plugin == pl) continue;
			if (pl.dependencies.contains(plugin)) {
				dependantsSet.add(pl);
				collectDependants(pl, dependants);
			}
		}
	}

	@Nullable
	PluginContainer<?> getContainer(@Nonnull String id) {
		LoadedPlugin plugin = plugins.get(id);
		if (plugin == null) {
			return null;
		}
		return plugin.container;
	}

	@Nonnull
	Collection<PluginContainer<?>> plugins() {
		return Collections2.transform(plugins.values(), plugin -> plugin.container);
	}

	@Nonnull
	Iterator<PluginClassLoaderImpl> getDependencies(@Nonnull String id) {
		var loaded = plugins.get(id);
		if (loaded == null) {
			return Collections.emptyIterator();
		}
		return Iterators.transform(loaded.dependencies.iterator(), input -> ((PluginClassLoaderImpl) input.container.plugin().getClass().getClassLoader()));
	}

	private void enable(LoadedPlugin loadedPlugin) throws PluginException {
		for (LoadedPlugin dependency : loadedPlugin.dependencies) {
			enable(dependency);
		}
		//noinspection rawtypes
		PluginContainerImpl container = loadedPlugin.container;
		Plugin plugin = container.plugin;
		if (plugin != null) return;
		try {
			//noinspection unchecked
			Class<? extends Plugin> pluginClass = (Class<? extends Plugin>) container.classLoader.lookupClass(container.preparedPlugin.pluginClassName());
			plugin = classAllocator.instance(pluginClass);
			plugin.onEnable();
			container.plugin = plugin;
		} catch (Exception ex) {
			throw new PluginException(ex);
		}
	}
}
