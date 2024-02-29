package software.coley.recaf.services.mapping.gen.naming;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link NameGeneratorProviders}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class NameGeneratorProvidersConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public NameGeneratorProvidersConfig() {
		super(ConfigGroups.SERVICE_MAPPING, NameGeneratorProviders.SERVICE_ID);
	}
}
