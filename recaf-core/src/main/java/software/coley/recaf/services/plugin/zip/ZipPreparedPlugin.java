package software.coley.recaf.services.plugin.zip;

import jakarta.annotation.Nonnull;
import software.coley.recaf.plugin.PluginSource;
import software.coley.recaf.plugin.PluginException;
import software.coley.recaf.plugin.PluginInfo;
import software.coley.recaf.services.plugin.PreparedPlugin;

import java.io.IOException;

final class ZipPreparedPlugin implements PreparedPlugin {
	private final PluginInfo pluginInfo;
	private final String pluginClassName;
	private final ZipSource classLoader;

	ZipPreparedPlugin(PluginInfo pluginInfo, String pluginClassName, ZipSource classLoader) {
		this.pluginInfo = pluginInfo;
		this.pluginClassName = pluginClassName;
		this.classLoader = classLoader;
	}

	@Nonnull
	@Override
	public PluginInfo info() {
		return pluginInfo;
	}

	@Nonnull
	@Override
	public PluginSource pluginSource() {
		return classLoader;
	}

	@Nonnull
	@Override
	public String pluginClassName() {
		return pluginClassName;
	}

	@Override
	public void reject() throws PluginException {
		try {
			classLoader.close();
		} catch (IOException ex) {
			throw new PluginException(ex);
		}
	}
}
