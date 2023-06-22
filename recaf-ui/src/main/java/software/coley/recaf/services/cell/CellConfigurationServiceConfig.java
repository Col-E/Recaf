package software.coley.recaf.services.cell;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link CellConfigurationService}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class CellConfigurationServiceConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public CellConfigurationServiceConfig() {
		super(ConfigGroups.SERVICE_UI, CellConfigurationService.SERVICE_ID + CONFIG_SUFFIX);
	}
}
