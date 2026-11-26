package software.coley.recaf.services.plugin;

import com.google.common.collect.Collections2;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.inject.spi.BeanManager;
import software.coley.recaf.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Plugin dependency graph.
 *
 * @author xDark
 */
final class PluginGraph {
	final Map<String, LoadedPlugin> plugins = HashMap.newHashMap(16);
	final BeanManager parentBeanManager;

	/**
	 * @param parentBeanManager
	 * 		Parent bean manager to use for plugin CDI runtimes.
	 */
	PluginGraph(@Nonnull BeanManager parentBeanManager) {
		this.parentBeanManager = parentBeanManager;
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
		try {
			for (var preparedPlugin : preparedPlugins) {
				String id = preparedPlugin.info().id();
				if (plugins.containsKey(id))
					throw new PluginException("Plugin %s is already loaded".formatted(id));
				var threadContextClassLoader = Thread.currentThread().getContextClassLoader();
				var parentLoader = threadContextClassLoader != null ? threadContextClassLoader : ClassLoader.getSystemClassLoader();
				var classLoader = new PluginClassLoaderImpl(parentLoader, preparedPlugin.pluginSource(), id);
				LoadedPlugin loadedPlugin = new LoadedPlugin(new PluginContainerImpl<>(preparedPlugin, classLoader));

				if (temp.putIfAbsent(id, loadedPlugin) != null)
					throw new PluginException("Duplicate plugin %s".formatted(id));
			}
			resolveDependencies(temp);
			wireDependencyClassLoaders(temp.values());
			for (LoadedPlugin loadedPlugin : temp.values())
				enable(loadedPlugin, initializedPlugins);

			plugins.putAll(temp);
			return Collections2.transform(temp.values(), input -> input.getContainer());
		} catch (PluginException failure) {
			rollbackFailedApply(temp, initializedPlugins, preparedPlugins, failure);
			throw failure;
		}
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
				List<Error> fatalErrors = new ArrayList<>();
				PluginException ex = unload(plugin, dependants, new HashSet<>(), fatalErrors);
				if (!fatalErrors.isEmpty()) {
					Error fatal = fatalErrors.getFirst();
					for (int i = 1; i < fatalErrors.size(); i++)
						fatal.addSuppressed(fatalErrors.get(i));
					if (ex != null)
						fatal.addSuppressed(ex);
					throw fatal;
				}
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
	 *		Plugin to unload.
	 * @param dependants
	 *		Map of plugin dependents.
	 * @param unloaded
	 *		Plugins already unloaded in this operation.
	 * @param fatalErrors
	 *		Errors encountered while continuing cleanup.
	 *
	 * @return Exception to be thrown if the plugin could not be unloaded.
	 */
	@Nullable
	private PluginException unload(@Nonnull LoadedPlugin plugin, @Nonnull Map<LoadedPlugin, Set<LoadedPlugin>> dependants,
	                               @Nonnull Set<LoadedPlugin> unloaded, @Nonnull List<Error> fatalErrors) {
		if (!unloaded.add(plugin))
			return null;

		String id = plugin.getContainer().info().id();
		if (!plugins.remove(id, plugin))
			throw new IllegalStateException("Plugin %s was already removed, recursion?".formatted(id));

		// Unload dependants first, so that the plugin can be safely unloaded.
		PluginException exception = null;
		for (LoadedPlugin dependant : dependants.getOrDefault(plugin, Collections.emptySet())) {
			PluginException inner = unload(dependant, dependants, unloaded, fatalErrors);
			if (inner != null) {
				if (exception == null)
					exception = inner;
				else
					exception.addSuppressed(inner);
			}
		}

		// Cleanup the plugin itself.
		Throwable cleanupFailure = cleanupPlugin(plugin.getContainer(), true);
		if (cleanupFailure instanceof Error error) {
			fatalErrors.add(error);
		} else if (cleanupFailure != null) {
			PluginException pluginFailure = new PluginException(cleanupFailure);
			if (exception == null)
				exception = pluginFailure;
			else
				exception.addSuppressed(pluginFailure);
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
	 *
	 * @throws IllegalStateException
	 * 		If a circular dependency is detected.
	 */
	private void collectDependants(@Nonnull LoadedPlugin plugin, @Nonnull Map<LoadedPlugin, Set<LoadedPlugin>> dependants) {
		collectDependants(plugin, dependants, new LinkedHashSet<>());
	}

	/**
	 * Recursively collects dependants while protecting against circular dependencies.
	 *
	 * @param plugin
	 * 		Plugin to collect dependants of in the current step of recursion.
	 * @param dependants
	 * 		Map to store results in.
	 * @param visiting
	 * 		Set of plugins visited in the current traversal path to detect circular dependencies.
	 *
	 * @throws IllegalStateException
	 * 		If a circular dependency is detected during traversal.
	 */
	private void collectDependants(@Nonnull LoadedPlugin plugin, @Nonnull Map<LoadedPlugin, Set<LoadedPlugin>> dependants, @Nonnull Set<LoadedPlugin> visiting) {
		if (!visiting.add(plugin)) {
			throw new IllegalStateException("Circular dependency detected at plugin: %s".formatted(plugin.getContainer().info().id()));
		}
		Set<LoadedPlugin> dependantsSet = dependants.computeIfAbsent(plugin, _ -> HashSet.newHashSet(4));
		for (LoadedPlugin pl : plugins.values()) {
			if (plugin == pl) continue;
			if (pl.getDependencies().contains(plugin)) {
				dependantsSet.add(pl);
				collectDependants(pl, dependants);
			}
		}
		visiting.remove(plugin);
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
	private void enable(@Nonnull LoadedPlugin loadedPlugin, @Nonnull List<LoadedPlugin> initializedPlugins) throws PluginException {
		enable(loadedPlugin, initializedPlugins, new LinkedHashSet<>());
	}

	/**
	 * Enables the given plugin, initializing if necessary.
	 *
	 * @param loadedPlugin
	 * 		Plugin to enable.
	 * @param initializedPlugins
	 * 		Plugins initialized during the current apply operation.
	 * @param visiting
	 * 		Set of plugins currently being initialized in the current dependency chain
	 * 		to detect circular dependencies.
	 *
	 * @throws PluginException
	 * 		If the plugin could not be initialized or enabled.
	 */
	@SuppressWarnings("rawtypes")
	private void enable(@Nonnull LoadedPlugin loadedPlugin, @Nonnull List<LoadedPlugin> initializedPlugins, @Nonnull Set<LoadedPlugin> visiting) throws PluginException {
		// Check if the plugin is already initialized.
		PluginContainerImpl container = loadedPlugin.getContainer();
		Plugin plugin = container.plugin;
		if (plugin != null) return; // Already initialized, skip.
		if (!visiting.add(loadedPlugin)) {
			throw new PluginException("Circular dependency detected during enablement at: %s".formatted(container.info().id()));
		}

		// Initialize and enable the plugin.
		try {
			// Enable dependent plugins
			for (LoadedPlugin dependency : loadedPlugin.getDependencies())
				enable(dependency, initializedPlugins, visiting);

			// Initialize the plugin runtime.
			PluginCdiContainer runtime = PluginCdiContainer.create(container.preparedPlugin.pluginSource(),
					(PluginClassLoaderImpl) container.classLoader, parentBeanManager);
			container.container = runtime;

			// Lookup the plugin class and create an instance.
			Class<? extends Plugin> pluginClass = container.classLoader
					.lookupClass(container.preparedPlugin.pluginClassName())
					.asSubclass(Plugin.class);
			plugin = runtime.instance(pluginClass);

			// Store the plugin instance and mark it as initialized.
			container.plugin = plugin;
			initializedPlugins.add(loadedPlugin);
			plugin.onEnable();
		} catch (PluginException ex) {
			throw ex;
		} catch (Throwable t) {
			throw new PluginException(t);
		} finally {
			visiting.remove(loadedPlugin);
		}
	}

	/**
	 * Rolls back plugins initialized during a failed apply operation.
	 *
	 * @param temp
	 * 		Staged plugins.
	 * @param initializedPlugins
	 * 		Plugins initialized before the failure.
	 * @param preparedPlugins
	 * 		All staged plugins whose sources must be rejected after runtime cleanup.
	 * @param failure
	 * 		Failure to attach rollback errors to.
	 */
	private void rollbackFailedApply(@Nonnull Map<String, LoadedPlugin> temp, @Nonnull List<LoadedPlugin> initializedPlugins,
	                                @Nonnull List<PreparedPlugin> preparedPlugins, @Nonnull PluginException failure) {
		Set<LoadedPlugin> cleanedPlugins = new HashSet<>();
		for (int i = initializedPlugins.size() - 1; i >= 0; i--) {
			LoadedPlugin loadedPlugin = initializedPlugins.get(i);
			appendCleanupFailure(failure, cleanupPlugin(loadedPlugin.getContainer(), false));
			cleanedPlugins.add(loadedPlugin);
		}

		// A runtime can exist even when entry-point lookup or allocation failed.
		for (LoadedPlugin loadedPlugin : temp.values()) {
			if (cleanedPlugins.contains(loadedPlugin) || loadedPlugin.getContainer().container == null)
				continue;
			appendCleanupFailure(failure, cleanupPlugin(loadedPlugin.getContainer(), false));
		}

		// Rejected prepared plugins own their source cleanup. Do this only after every child runtime is stopped.
		for (PreparedPlugin preparedPlugin : preparedPlugins) {
			try {
				preparedPlugin.reject();
			} catch (Throwable ex) {
				appendCleanupFailure(failure, ex);
			}
		}
	}

	/**
	 * Cleans up a plugin container, shutting down its runtime and closing its classloader.
	 *
	 * @param container
	 * 		Plugin container to clean up.
	 * @param closeSource
	 * 		If the plugin source should be closed.
	 *
	 * @return Failure encountered during cleanup, or {@code null} if cleanup was successful.
	 */
	@Nullable
	private static Throwable cleanupPlugin(@Nonnull PluginContainerImpl<?> container, boolean closeSource) {
		Throwable failure = null;
		Plugin plugin = container.plugin;
		PluginCdiContainer runtime = container.container;

		// Cleanup the plugin runtime and instance.
		if (plugin != null) {
			try {
				plugin.onDisable();
			} catch (Throwable ex) {
				failure = appendFailure(failure, ex);
			}
			if (runtime != null) {
				try {
					runtime.release(plugin);
				} catch (Throwable ex) {
					failure = appendFailure(failure, ex);
				}
			}
		}

		// Tear down the plugin container.
		container.plugin = null;
		container.container = null;
		if (runtime != null) {
			try {
				runtime.shutdown();
			} catch (Throwable ex) {
				failure = appendFailure(failure, ex);
			}
		}

		// Close the plugin source and classloader.
		if (closeSource && container.preparedPlugin.pluginSource() instanceof AutoCloseable closeable) {
			try {
				closeable.close();
			} catch (Throwable ex) {
				failure = appendFailure(failure, ex);
			}
		}
		if (container.classLoader instanceof AutoCloseable closeable) {
			try {
				closeable.close();
			} catch (Throwable ex) {
				failure = appendFailure(failure, ex);
			}
		}
		return failure;
	}

	/**
	 * Appends a cleanup failure to the given plugin exception.
	 *
	 * @param failure
	 * 		Plugin exception to append to.
	 * @param cleanupFailure
	 * 		Cleanup failure to append.
	 */
	private static void appendCleanupFailure(@Nonnull PluginException failure, @Nullable Throwable cleanupFailure) {
		if (cleanupFailure != null && cleanupFailure != failure)
			failure.addSuppressed(cleanupFailure);
	}

	/**
	 * @param failure
	 * 		Failure to append to.
	 * @param additionalFailure
	 * 		Failure to append.
	 *
	 * @return Failure with the additional failure appended, or the additional failure if the first was null.
	 */
	@Nonnull
	private static Throwable appendFailure(@Nullable Throwable failure, @Nonnull Throwable additionalFailure) {
		if (failure == null)
			return additionalFailure;
		if (failure != additionalFailure)
			failure.addSuppressed(additionalFailure);
		return failure;
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
