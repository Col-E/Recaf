package software.coley.recaf.services.search;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link SearchService}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class SearchServiceConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public SearchServiceConfig() {
		super(ConfigGroups.SERVICE_ANALYSIS, SearchService.SERVICE_ID + CONFIG_SUFFIX);
	}
}
