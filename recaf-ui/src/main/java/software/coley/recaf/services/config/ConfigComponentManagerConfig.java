package software.coley.recaf.services.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link ConfigComponentManager}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ConfigComponentManagerConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public ConfigComponentManagerConfig() {
		super(ConfigGroups.SERVICE_UI, ConfigComponentManager.ID + CONFIG_SUFFIX);
	}
}
