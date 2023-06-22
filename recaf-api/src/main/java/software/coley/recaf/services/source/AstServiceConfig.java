package software.coley.recaf.services.source;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link AstService}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AstServiceConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public AstServiceConfig() {
		super(ConfigGroups.SERVICE_ANALYSIS, AstService.ID + CONFIG_SUFFIX);
	}
}
