package software.coley.recaf.services.plugin;

import com.google.common.collect.Collections2;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.plugin.Plugin;

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
		List<LoadedPlugin> initializedPlugins = new ArrayList<>(preparedPlugins.size());
		var plugins = this.plugins;
		for (var preparedPlugin : preparedPlugins) {
			String id = preparedPlugin.info().id();
			if (plugins.containsKey(id)) {
				throw new PluginException("Plugin %s is already loaded".formatted(id));
			}
			var threadContextClassLoader = Thread.currentThread().getContextClassLoader();
			var parentLoader = threadContextClassLoader != null ? threadContextClassLoader : ClassLoader.getSystemClassLoader();
			var classLoader = new PluginClassLoaderImpl(parentLoader, preparedPlugin.pluginSource(), id);
			LoadedPlugin loadedPlugin = new LoadedPlugin(new PluginContainerImpl<>(preparedPlugin, classLoader));

			if (temp.putIfAbsent(id, loadedPlugin) != null) {
				throw new PluginException("Duplicate plugin %s".formatted(id));
			}
		}
		resolveDependencies(temp);
		wireDependencyClassLoaders(temp.values());
		for (LoadedPlugin loadedPlugin : temp.values()) {
			try {
				enable(loadedPlugin, initializedPlugins);
			} catch (PluginException ex) {
				rollbackFailedApply(temp, initializedPlugins, ex);
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
				PluginException ex = unload(plugin, dependants, new HashSet<>());
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
	 * Resolves hard and soft dependencies for all staged plugins.
	 *
	 * @param temp
	 * 		Staged plugins.
	 *
	 * @throws PluginException
	 * 		When a hard dependency is missing.
	 */
	private void resolveDependencies(@Nonnull Map<String, LoadedPlugin> temp) throws PluginException {
		for (LoadedPlugin plugin : temp.values()) {
			PluginInfo info = plugin.getContainer().info();

			for (String dependencyId : info.dependencies()) {
				if (info.id().equals(dependencyId)) {
					throw new PluginException("Plugin %s cannot depend of itself".formatted(info.id()));
				}
				LoadedPlugin dependency = resolveDependency(dependencyId, temp);
				if (dependency == null) {
					throw new PluginException("Plugin %s is missing dependency %s".formatted(info.id(), dependencyId));
				}
				addDependency(plugin, dependency);
			}

			for (String dependencyId : info.softDependencies()) {
				if (info.id().equals(dependencyId)) {
					continue;
				}
				LoadedPlugin dependency = resolveDependency(dependencyId, temp);
				if (dependency != null) {
					addDependency(plugin, dependency);
				}
			}
		}
	}

	/**
	 * Wires already resolved dependencies into plugin classloaders.
	 *
	 * @param loadedPlugins
	 * 		Plugins to wire.
	 */
	private static void wireDependencyClassLoaders(@Nonnull Collection<LoadedPlugin> loadedPlugins) {
		for (LoadedPlugin loadedPlugin : loadedPlugins) {
			PluginClassLoaderImpl classLoader = classLoaderOf(loadedPlugin);
			List<PluginClassLoaderImpl> dependencyLoaders = loadedPlugin.getDependencies()
					.stream()
					.map(PluginGraph::classLoaderOf)
					.toList();

			classLoader.setDependencyClassLoaders(dependencyLoaders);
		}
	}

	/**
	 * @param id
	 * 		Dependency plugin identifier.
	 * @param temp
	 * 		Staged plugins.
	 *
	 * @return Resolved dependency from staged or already loaded plugins.
	 */
	@Nullable
	private LoadedPlugin resolveDependency(@Nonnull String id, @Nonnull Map<String, LoadedPlugin> temp) {
		LoadedPlugin dependency = temp.get(id);
		if (dependency != null) {
			return dependency;
		}
		return plugins.get(id);
	}

	/**
	 * Adds dependency once.
	 *
	 * @param plugin
	 * 		Plugin that owns the dependency.
	 * @param dependency
	 * 		Dependency to add.
	 */
	private static void addDependency(@Nonnull LoadedPlugin plugin, @Nonnull LoadedPlugin dependency) {
		plugin.getDependencies().add(dependency);
	}

	/**
	 * Attempts to unload the given plugin, along with its dependants.
	 *
	 * @param plugin
	 * 		Plugin to unload.
	 * @param dependants
	 * 		Map of plugin dependents.
	 * @param unloaded
	 * 		Plugins already unloaded in this operation.
	 *
	 * @return Exception to be thrown if the plugin could not be unloaded.
	 */
	@Nullable
	private PluginException unload(@Nonnull LoadedPlugin plugin, @Nonnull Map<LoadedPlugin, Set<LoadedPlugin>> dependants, @Nonnull Set<LoadedPlugin> unloaded) {
		if (!unloaded.add(plugin)) {
			return null;
		}
		String id = plugin.getContainer().info().id();
		if (!plugins.remove(id, plugin)) {
			throw new IllegalStateException("Plugin %s was already removed, recursion?".formatted(id));
		}
		PluginException exception = null;
		for (LoadedPlugin dependant : dependants.getOrDefault(plugin, Collections.emptySet())) {
			PluginException inner = unload(dependant, dependants, unloaded);
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
		Set<LoadedPlugin> dependantsSet = dependants.computeIfAbsent(plugin, _ -> HashSet.newHashSet(4));
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
	 * Enables the given plugin, initializing if necessary.
	 *
	 * @param loadedPlugin
	 * 		Plugin to enable.
	 * @param initializedPlugins
	 * 		Plugins initialized during the current apply operation.
	 *
	 * @throws PluginException
	 * 		If the plugin could not be initialized or enabled.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private void enable(@Nonnull LoadedPlugin loadedPlugin, @Nonnull List<LoadedPlugin> initializedPlugins) throws PluginException {
		// Enable dependent plugins
		for (LoadedPlugin dependency : loadedPlugin.getDependencies())
			enable(dependency, initializedPlugins);

		// Check if the plugin is already initialized.
		PluginContainerImpl container = loadedPlugin.getContainer();
		Plugin plugin = container.plugin;
		if (plugin != null) return; // Already initialized, skip.

		// Initialize and enable the plugin.
		try {
			Class<? extends Plugin> pluginClass = (Class<? extends Plugin>) container.classLoader.lookupClass(container.preparedPlugin.pluginClassName());
			plugin = classAllocator.instance(pluginClass);
			container.plugin = plugin;
			initializedPlugins.add(loadedPlugin);
			plugin.onEnable();
		} catch (Throwable t) {
			throw new PluginException(t);
		}
	}

	/**
	 * Rolls back plugins initialized during a failed apply operation.
	 *
	 * @param temp
	 * 		Staged plugins.
	 * @param initializedPlugins
	 * 		Plugins initialized before the failure.
	 * @param failure
	 * 		Failure to attach rollback errors to.
	 */
	private static void rollbackFailedApply(@Nonnull Map<String, LoadedPlugin> temp, @Nonnull List<LoadedPlugin> initializedPlugins, @Nonnull PluginException failure) {
		for (int i = initializedPlugins.size() - 1; i >= 0; i--) {
			LoadedPlugin loadedPlugin = initializedPlugins.get(i);
			PluginContainerImpl<?> container = loadedPlugin.getContainer();

			try {
				Plugin plugin = container.plugin;
				if (plugin != null) {
					plugin.onDisable();
				}
			} catch (Exception ex) {
				failure.addSuppressed(ex);
			} finally {
				container.plugin = null;

				if (container.classLoader instanceof AutoCloseable closeable) {
					try {
						closeable.close();
					} catch (Exception ex) {
						failure.addSuppressed(ex);
					}
				}
			}
		}

		for (LoadedPlugin loadedPlugin : temp.values()) {
			try {
				loadedPlugin.getContainer().preparedPlugin.reject();
			} catch (Exception ex) {
				failure.addSuppressed(ex);
			}
		}
	}

	/**
	 * @param plugin
	 * 		Plugin to get classloader from.
	 *
	 * @return Plugin classloader.
	 */
	@Nonnull
	private static PluginClassLoaderImpl classLoaderOf(@Nonnull LoadedPlugin plugin) {
		return (PluginClassLoaderImpl) plugin.getContainer().classLoader;
	}
}
