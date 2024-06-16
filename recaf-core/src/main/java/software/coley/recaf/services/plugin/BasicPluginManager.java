package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.cdi.EagerInitialization;
import software.coley.recaf.plugin.*;
import software.coley.recaf.services.plugin.discovery.DiscoveredPluginSource;
import software.coley.recaf.services.plugin.discovery.PluginDiscoverer;
import software.coley.recaf.services.plugin.zip.ZipPluginLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Basic implementation of {@link PluginManager}.
 *
 * @author xDark
 */
@ApplicationScoped
@EagerInitialization
public class BasicPluginManager implements PluginManager {
	private final List<PluginLoader> loaders = new ArrayList<>();
	private final PluginGraph mainGraph;
	private final ClassAllocator classAllocator;
	private final PluginManagerConfig config;

	@Inject
	public BasicPluginManager(PluginManagerConfig config, CdiClassAllocator classAllocator) {
		this.classAllocator = classAllocator;
		this.config = config;
		mainGraph = new PluginGraph(classAllocator);

		// Add ZIP/JAR loading
		registerLoader(new ZipPluginLoader());
	}

	@Nonnull
	@Override
	public ClassAllocator getAllocator() {
		return classAllocator;
	}

	@Override
	@Nullable
	@SuppressWarnings("unchecked")
	public <T extends Plugin> PluginContainer<T> getPlugin(@Nonnull String id) {
		return (PluginContainer<T>) mainGraph.getContainer(id);
	}

	@Nonnull
	@Override
	public Collection<PluginContainer<?>> getPlugins() {
		return mainGraph.plugins();
	}

	@Override
	public void registerLoader(@Nonnull PluginLoader loader) {
		loaders.add(loader);
	}

	@Nonnull
	@Override
	public Collection<PluginContainer<?>> loadPlugins(@Nonnull PluginDiscoverer discoverer) throws PluginException {
		List<DiscoveredPluginSource> discoveredPlugins = discoverer.findSources();
		List<PluginLoader> loaders = this.loaders;
		List<PreparedPlugin> prepared = new ArrayList<>(discoveredPlugins.size());
		for (DiscoveredPluginSource plugin : discoveredPlugins) {
			for (PluginLoader loader : loaders) {
				PreparedPlugin preparedPlugin = loader.prepare(plugin.source());
				if (preparedPlugin == null)
					continue;
				prepared.add(preparedPlugin);
			}
		}
		return mainGraph.apply(prepared);
	}

	@Nonnull
	@Override
	public PluginUnloader unloaderFor(@Nonnull String id) {
		return mainGraph.unloaderFor(id);
	}

	@Override
	public boolean isPluginLoaded(@Nonnull String id) {
		return mainGraph.getContainer(id) != null;
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
