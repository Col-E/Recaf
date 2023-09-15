package software.coley.recaf.services.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link ConfigManager}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ConfigManagerConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public ConfigManagerConfig() {
		super(ConfigGroups.SERVICE, ConfigManager.SERVICE_ID + CONFIG_SUFFIX);
	}
}
