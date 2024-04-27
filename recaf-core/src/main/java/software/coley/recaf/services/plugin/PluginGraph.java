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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PluginGraph {
	final Map<String, LoadedPlugin> plugins = HashMap.newHashMap(16);
	private final ClassAllocator classAllocator;

	PluginGraph(ClassAllocator classAllocator) {
		this.classAllocator = classAllocator;
	}

	// FIXME pull in jgrapht (or make a small alternative). This is a mess.
	Collection<PluginContainer<?>> apply(List<PreparedPlugin> preparedPlugins) throws PluginException {
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
					Plugin maybeEnabled = pl.container.plugin;
					if (maybeEnabled != null) {
						try {
							maybeEnabled.onDisable();
						} catch (Exception ex1) {
							ex.addSuppressed(ex1);
						}
					}
				}
				throw ex;
			}
		}
		plugins.putAll(temp);
		return Collections2.transform(temp.values(), input -> input.container);
	}

	@Nullable
	PluginContainer<?> getContainer(@Nonnull String id) {
		LoadedPlugin plugin = plugins.get(id);
		if (plugin == null) {
			return null;
		}
		return plugin.container;
	}

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
