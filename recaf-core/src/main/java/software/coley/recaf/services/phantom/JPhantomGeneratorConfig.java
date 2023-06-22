package software.coley.recaf.services.phantom;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link JPhantomGenerator}
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class JPhantomGeneratorConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public JPhantomGeneratorConfig() {
		super(ConfigGroups.SERVICE_ANALYSIS, JPhantomGenerator.SERVICE_ID + CONFIG_SUFFIX);
	}
}
