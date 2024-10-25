package software.coley.recaf.services.window;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link WindowStyling}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class WindowStylingConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public WindowStylingConfig() {
		super(ConfigGroups.SERVICE_UI, WindowStyling.SERVICE_ID + CONFIG_SUFFIX);
	}
}
