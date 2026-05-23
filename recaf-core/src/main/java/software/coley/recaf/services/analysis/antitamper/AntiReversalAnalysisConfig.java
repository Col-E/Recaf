package software.coley.recaf.services.analysis.antitamper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link AntiReversalAnalysisService}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AntiReversalAnalysisConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public AntiReversalAnalysisConfig() {
		super(ConfigGroups.SERVICE_ANALYSIS, AntiReversalAnalysisService.SERVICE_ID + CONFIG_SUFFIX);
	}
}
