package software.coley.recaf.services.callgraph;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link CallGraph}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class CallGraphConfig extends BasicConfigContainer implements ServiceConfig {

	@Inject
	public CallGraphConfig() {
		super(ConfigGroups.SERVICE_ANALYSIS, CallGraphService.SERVICE_ID + CONFIG_SUFFIX);
	}
}
