package software.coley.recaf.services.cell;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link ContextMenuProviderService}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ContextMenuProviderServiceConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public ContextMenuProviderServiceConfig() {
		super(ConfigGroups.SERVICE_UI, ContextMenuProviderService.SERVICE_ID + CONFIG_SUFFIX);
	}
}
