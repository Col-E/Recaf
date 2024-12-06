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
public class InheritanceGraphConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public InheritanceGraphConfig() {
		super(ConfigGroups.SERVICE_ANALYSIS, InheritanceGraph.SERVICE_ID + CONFIG_SUFFIX);
	}
}
