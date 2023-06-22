package software.coley.recaf.services.mapping.aggregate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link AggregateMappingManager}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AggregateMappingManagerConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public AggregateMappingManagerConfig() {
		super(ConfigGroups.SERVICE_MAPPING, AggregateMappingManager.SERVICE_ID + CONFIG_SUFFIX);
	}
}
