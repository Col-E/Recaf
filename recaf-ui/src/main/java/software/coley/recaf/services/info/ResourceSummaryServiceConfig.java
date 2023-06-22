package software.coley.recaf.services.info;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link ResourceSummaryService}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ResourceSummaryServiceConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public ResourceSummaryServiceConfig() {
		super(ConfigGroups.SERVICE_UI, ResourceSummaryService.SERVICE_ID + CONFIG_SUFFIX);
	}
}
