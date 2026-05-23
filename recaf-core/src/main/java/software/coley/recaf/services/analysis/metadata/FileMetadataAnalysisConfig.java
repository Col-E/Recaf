package software.coley.recaf.services.analysis.metadata;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link FileMetadataAnalysisService}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class FileMetadataAnalysisConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public FileMetadataAnalysisConfig() {
		super(ConfigGroups.SERVICE_ANALYSIS, FileMetadataAnalysisService.SERVICE_ID + CONFIG_SUFFIX);
	}
}
