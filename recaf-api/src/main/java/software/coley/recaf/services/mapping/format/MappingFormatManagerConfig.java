package software.coley.recaf.services.mapping.format;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link MappingFormatManager}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class MappingFormatManagerConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public MappingFormatManagerConfig() {
		super(ConfigGroups.SERVICE_MAPPING, MappingFormatManager.SERVICE_ID + CONFIG_SUFFIX);
	}
}
