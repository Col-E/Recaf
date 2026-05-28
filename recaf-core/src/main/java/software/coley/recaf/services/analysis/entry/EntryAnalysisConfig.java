package software.coley.recaf.services.analysis.entry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link EntryAnalysisService}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class EntryAnalysisConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public EntryAnalysisConfig() {
		super(ConfigGroups.SERVICE_ANALYSIS, EntryAnalysisService.SERVICE_ID + CONFIG_SUFFIX);
	}
}
