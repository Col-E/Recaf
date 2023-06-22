package software.coley.recaf.services.navigation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link Actions}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ActionsConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public ActionsConfig() {
		super(ConfigGroups.SERVICE_UI, Actions.ID + CONFIG_SUFFIX);
	}
}
