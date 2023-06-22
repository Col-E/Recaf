package software.coley.recaf.workspace;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link WorkspaceManager}
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class WorkspaceManagerConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public WorkspaceManagerConfig() {
		super(ConfigGroups.SERVICE_IO, WorkspaceManager.SERVICE_ID + CONFIG_SUFFIX);
	}
}