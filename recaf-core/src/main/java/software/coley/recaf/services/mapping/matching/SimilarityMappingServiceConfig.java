package software.coley.recaf.services.mapping.matching;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link SimilarityMappingService}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class SimilarityMappingServiceConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public SimilarityMappingServiceConfig() {
		super(ConfigGroups.SERVICE_MAPPING, SimilarityMappingService.SERVICE_ID + CONFIG_SUFFIX);
	}
}
