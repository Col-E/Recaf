package software.coley.recaf.services.search;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link SimilaritySearchService}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class SimilaritySearchServiceConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public SimilaritySearchServiceConfig() {
		super(ConfigGroups.SERVICE_ANALYSIS, SimilaritySearchService.SERVICE_ID + CONFIG_SUFFIX);
	}
}
