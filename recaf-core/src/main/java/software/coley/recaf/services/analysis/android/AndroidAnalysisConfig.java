package software.coley.recaf.services.analysis.android;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link AndroidAnalysisService}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AndroidAnalysisConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public AndroidAnalysisConfig() {
		super(ConfigGroups.SERVICE_ANALYSIS, AndroidAnalysisService.SERVICE_ID + CONFIG_SUFFIX);
	}
}
