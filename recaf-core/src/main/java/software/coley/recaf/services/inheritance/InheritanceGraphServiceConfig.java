package software.coley.recaf.services.inheritance;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link InheritanceGraphService}
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class InheritanceGraphServiceConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public InheritanceGraphServiceConfig() {
		super(ConfigGroups.SERVICE_ANALYSIS, InheritanceGraphService.SERVICE_ID + CONFIG_SUFFIX);
	}
}
