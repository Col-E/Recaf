package software.coley.recaf.services.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link ConfigIconManager}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ConfigIconManagerConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public ConfigIconManagerConfig() {
		super(ConfigGroups.SERVICE_UI, ConfigIconManager.ID + CONFIG_SUFFIX);
	}
}
