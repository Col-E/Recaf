package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;

import java.util.HashSet;
import java.util.Set;

/**
 * Model of a loaded plugin and its dependence for {@link PluginGraph}.
 *
 * @author xDark
 */
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
	 * @return Container the loaded plugin belongs to.
	 */
	@Nonnull
	public PluginContainerImpl<?> getContainer() {
		return container;
	}

	@Override
	public String toString() {
		return "LoadedPlugin{" + container.info().id() + '}';
	}
}
