package software.coley.recaf.services.workspace;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link WorkspaceProcessingService}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class WorkspaceProcessingConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public WorkspaceProcessingConfig() {
		super(ConfigGroups.SERVICE_TRANSFORM, WorkspaceProcessingService.SERVICE_ID + CONFIG_SUFFIX);
	}
}
