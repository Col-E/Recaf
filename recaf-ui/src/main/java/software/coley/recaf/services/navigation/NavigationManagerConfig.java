package software.coley.recaf.services.navigation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;
import software.coley.recaf.services.config.ConfigIconManager;

/**
 * Config for {@link NavigationManager}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class NavigationManagerConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public NavigationManagerConfig() {
		super(ConfigGroups.SERVICE_UI, NavigationManager.ID + CONFIG_SUFFIX);
	}
}
