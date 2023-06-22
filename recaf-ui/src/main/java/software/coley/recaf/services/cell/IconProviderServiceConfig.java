package software.coley.recaf.services.cell;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link IconProviderService}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class IconProviderServiceConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public IconProviderServiceConfig() {
		super(ConfigGroups.SERVICE_UI, IconProviderService.SERVICE_ID + CONFIG_SUFFIX);
	}
}
