package software.coley.recaf.services.plugin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link PluginManager}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class PluginManagerConfig extends BasicConfigContainer implements ServiceConfig {
	private boolean allowLocalScan = true;

	@Inject
	public PluginManagerConfig() {
		super(ConfigGroups.SERVICE_PLUGIN, PluginManager.SERVICE_ID + CONFIG_SUFFIX);
	}

	/**
	 * @return {@code true} when local plugins should be scanned when the plugin manager implementation initializes.
	 * {@code false} to disable local automatic plugin loading.
	 */
	public boolean isAllowLocalScan() {
		return allowLocalScan;
	}

	/**
	 * @param allowLocalScan
	 *        {@code true} when local plugins should be scanned when the plugin manager implementation initializes.
	 *        {@code false} to disable local automatic plugin loading.
	 */
	public void setAllowLocalScan(boolean allowLocalScan) {
		this.allowLocalScan = allowLocalScan;
	}
}
