package software.coley.recaf.services.plugin;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.plugin.*;

import java.util.*;
import java.util.stream.Stream;

/**
 * Plugin dependency graph.
 *
 * @author xDark
 */
final class PluginGraph {
	final Map<String, LoadedPlugin> plugins = HashMap.newHashMap(16);
	private final ClassAllocator classAllocator;

	/**
	 * @param classAllocator
	 * 		Allocator to construct plugin instances with.
	 */
	PluginGraph(@Nonnull ClassAllocator classAllocator) {
		this.classAllocator = classAllocator;
	}

	/**
	 * Primary plugin load action.
	 *
	 * @param preparedPlugins
	 * 		Intermediate plugin data to load.
	 *
	 * @return Collection of loaded plugin containers.
	 *
	 * @throws PluginException
	 * 		If the plugins could not be loaded for any reason.
	 */
	@Nonnull
	Collection<PluginContainer<?>> apply(@Nonnull List<PreparedPlugin> preparedPlugins) throws PluginException {
		Map<String, LoadedPlugin> temp = LinkedHashMap.newLinkedHashMap(preparedPlugins.size());
		var plugins = this.plugins;
		for (var preparedPlugin : preparedPlugins) {
			String id = preparedPlugin.info().id();
			if (plugins.containsKey(id)) {
				throw new PluginException("Plugin %s is already loaded".formatted(id));
			}
			var threadContextClassLoader = Thread.currentThread().getContextClassLoader();
			var parentLoader = threadContextClassLoader != null ? threadContextClassLoader : ClassLoader.getSystemClassLoader();
			var classLoader = new PluginClassLoaderImpl(parentLoader, this, preparedPlugin.pluginSource(), id);
			LoadedPlugin loadedPlugin = new LoadedPlugin(new PluginContainerImpl<>(preparedPlugin, classLoader));

			if (temp.putIfAbsent(id, loadedPlugin) != null) {
				throw new PluginException("Duplicate plugin %s".formatted(id));
			}
		}
		for (LoadedPlugin plugin : temp.values()) {
			PluginInfo info = plugin.getContainer().info();
			for (String dependencyId : info.dependencies()) {
				LoadedPlugin dep = temp.get(dependencyId);
				if (dep == null) {
					dep = plugins.get(dependencyId);
				}
				if (dep == null) {
					throw new PluginException("Plugin %s is missing dependency %s".formatted(info.id(), dependencyId));
				}
				plugin.getDependencies().add(dep);
			}
			for (String dependencyId : info.softDependencies()) {
				LoadedPlugin dep = temp.get(dependencyId);
				if (dep == null && (dep = plugins.get(dependencyId)) == null) {
					continue;
				}
				plugin.getDependencies().add(dep);
			}
		}
		for (LoadedPlugin loadedPlugin : temp.values()) {
			try {
				enable(loadedPlugin);
			} catch (PluginException ex) {
				for (LoadedPlugin pl : temp.values()) {
					PluginContainerImpl<?> container = pl.getContainer();
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
		return Collections2.transform(temp.values(), input -> input.getContainer());
	}

	/**
	 * @param id
	 * 		Plugin identifier.
	 *
	 * @return Plugin unload action.
	 */
	@Nonnull
	PluginUnloader unloaderFor(@Nonnull String id) {
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
				return plugin.getContainer().info();
			}

			@Nonnull
			@Override
			public Stream<PluginInfo> dependants() {
				return dependants.values()
						.stream()
						.flatMap(Collection::stream)
						.map(plugin -> plugin.getContainer().info());
			}
		};
	}

	/**
	 * Attempts to unload the given plugin, along with its dependants.
	 *
	 * @param plugin
	 * 		Plugin to unload.
	 * @param dependants
	 * 		Map of plugin dependents.
	 *
	 * @return Exception to be thrown if the plugin could not be unloaded.
	 */
	@Nullable
	private PluginException unload(@Nonnull LoadedPlugin plugin, @Nonnull Map<LoadedPlugin, Set<LoadedPlugin>> dependants) {
		String id = plugin.getContainer().info().id();
		if (!plugins.remove(id, plugin)) {
			throw new IllegalStateException("Plugin %s was already removed, recursion?".formatted(id));
		}
		PluginException exception = null;
		for (LoadedPlugin dependant : dependants.get(plugin)) {
			PluginException inner = unload(dependant, dependants);
			if (inner != null) {
				if (exception == null) {
					exception = inner;
				} else {
					exception.addSuppressed(inner);
				}
			}
		}
		try {
			PluginContainerImpl<?> container = plugin.getContainer();
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

	/**
	 * Iterates over which plugins depend on the given plugin, and stores them in the provided map.
	 *
	 * @param plugin
	 * 		Plugin to collect dependants of.
	 * @param dependants
	 * 		Map to store results in.
	 */
	private void collectDependants(@Nonnull LoadedPlugin plugin, @Nonnull Map<LoadedPlugin, Set<LoadedPlugin>> dependants) {
		Set<LoadedPlugin> dependantsSet = dependants.computeIfAbsent(plugin, __ -> HashSet.newHashSet(4));
		for (LoadedPlugin pl : plugins.values()) {
			if (plugin == pl) continue;
			if (pl.getDependencies().contains(plugin)) {
				dependantsSet.add(pl);
				collectDependants(pl, dependants);
			}
		}
	}

	/**
	 * @param id
	 * 		Plugin identifier.
	 *
	 * @return Plugin container if the item was found.
	 */
	@Nullable
	PluginContainer<?> getContainer(@Nonnull String id) {
		LoadedPlugin plugin = plugins.get(id);
		if (plugin == null)
			return null;
		return plugin.getContainer();
	}

	/**
	 * @return Collection of plugin containers.
	 */
	@Nonnull
	Collection<PluginContainer<?>> plugins() {
		return Collections2.transform(plugins.values(), LoadedPlugin::getContainer);
	}

	/**
	 * @param id
	 * 		Plugin identifier.
	 *
	 * @return Iterator of dependency classloaders.
	 */
	@Nonnull
	Iterator<PluginClassLoaderImpl> getDependencyClassloaders(@Nonnull String id) {
		var loaded = plugins.get(id);
		if (loaded == null) {
			return Collections.emptyIterator();
		}
		return Iterators.transform(loaded.getDependencies().iterator(), input -> ((PluginClassLoaderImpl) input.getContainer().plugin().getClass().getClassLoader()));
	}

	/**
	 * Enables the given plugin, initializing if necessary.
	 *
	 * @param loadedPlugin
	 * 		Plugin to enable.
	 *
	 * @throws PluginException
	 * 		If the plugin could not be initialized or enabled.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private void enable(@Nonnull LoadedPlugin loadedPlugin) throws PluginException {
		// Enable dependent plugins
		for (LoadedPlugin dependency : loadedPlugin.getDependencies())
			enable(dependency);

		// Check if the plugin is already initialized.
		PluginContainerImpl container = loadedPlugin.getContainer();
		Plugin plugin = container.plugin;
		if (plugin != null) return; // Already initialized, skip.

		// Initialize and enable the plugin.
		try {
			Class<? extends Plugin> pluginClass = (Class<? extends Plugin>) container.classLoader.lookupClass(container.preparedPlugin.pluginClassName());
			plugin = classAllocator.instance(pluginClass);
			plugin.onEnable();
			container.plugin = plugin;
		} catch (Throwable t) {
			throw new PluginException(t);
		}
	}
}
