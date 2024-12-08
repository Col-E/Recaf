package software.coley.recaf.services.mapping;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link MappingApplierService}
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class MappingApplierConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public MappingApplierConfig() {
		super(ConfigGroups.SERVICE_MAPPING, MappingApplierService.SERVICE_ID + CONFIG_SUFFIX);
	}
}