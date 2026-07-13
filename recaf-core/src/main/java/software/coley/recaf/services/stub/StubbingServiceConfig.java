package software.coley.recaf.services.stub;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link StubbingService}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class StubbingServiceConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public StubbingServiceConfig() {
		super(ConfigGroups.SERVICE_UI, StubbingService.SERVICE_ID + CONFIG_SUFFIX);
	}
}
