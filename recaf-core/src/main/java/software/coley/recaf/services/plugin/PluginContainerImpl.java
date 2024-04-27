package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import software.coley.recaf.plugin.Plugin;
import software.coley.recaf.plugin.PluginContainer;
import software.coley.recaf.plugin.PluginInfo;

final class PluginContainerImpl<P extends Plugin> implements PluginContainer<P> {
	final PreparedPlugin preparedPlugin;
	final PluginClassLoader classLoader;
	P plugin;

	PluginContainerImpl(PreparedPlugin preparedPlugin, PluginClassLoader classLoader) {
		this.preparedPlugin = preparedPlugin;
		this.classLoader = classLoader;
	}

	@Nonnull
	@Override
	public PluginInfo info() {
		return preparedPlugin.info();
	}

	@Nonnull
	@Override
	public P plugin() {
		P plugin = this.plugin;
		if (plugin == null) {
			throw new IllegalStateException("Uninitialized plugin");
		}
		return plugin;
	}
}
