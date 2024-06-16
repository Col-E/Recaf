package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;

import java.util.HashSet;
import java.util.Set;

final class LoadedPlugin {
	private final Set<LoadedPlugin> dependencies = HashSet.newHashSet(4);
	private final PluginContainerImpl<?> container;

	LoadedPlugin(@Nonnull PluginContainerImpl<?> container) {
		this.container = container;
	}

	/**
	 * @return Mutable set of dependencies this plugin relies on.
	 */
	@Nonnull
	public Set<LoadedPlugin> getDependencies() {
		return dependencies;
	}

	/**
	 * @return
	 */
	@Nonnull
	public PluginContainerImpl<?> getContainer() {
		return container;
	}
}
