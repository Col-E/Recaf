package software.coley.recaf.services.plugin;

import java.util.HashSet;
import java.util.Set;

final class LoadedPlugin {
	final Set<LoadedPlugin> dependencies = HashSet.newHashSet(4);
	final PluginContainerImpl<?> container;

	LoadedPlugin(PluginContainerImpl<?> container) {
		this.container = container;
	}
}
