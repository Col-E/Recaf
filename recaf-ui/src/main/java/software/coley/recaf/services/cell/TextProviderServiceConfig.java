package software.coley.recaf.services.cell;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link TextProviderService}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class TextProviderServiceConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public TextProviderServiceConfig() {
		super(ConfigGroups.SERVICE_UI, TextProviderService.SERVICE_ID + CONFIG_SUFFIX);
	}
}
