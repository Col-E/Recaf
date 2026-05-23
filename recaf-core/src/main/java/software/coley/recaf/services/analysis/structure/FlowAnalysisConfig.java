package software.coley.recaf.services.analysis.structure;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link FlowAnalysisService}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class FlowAnalysisConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public FlowAnalysisConfig() {
		super(ConfigGroups.SERVICE_ANALYSIS, FlowAnalysisService.SERVICE_ID + CONFIG_SUFFIX);
	}
}
