package software.coley.recaf.services.mapping;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link MappingListeners}
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class MappingListenersConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public MappingListenersConfig() {
		super(ConfigGroups.SERVICE_MAPPING, MappingListeners.SERVICE_ID + CONFIG_SUFFIX);
	}
}