package software.coley.recaf.services.window;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link WindowFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class WindowFactoryConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public WindowFactoryConfig() {
		super(ConfigGroups.SERVICE_UI, WindowFactory.SERVICE_ID + CONFIG_SUFFIX);
	}
}
