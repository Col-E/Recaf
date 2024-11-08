package software.coley.recaf.services.transform;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link TransformationManager}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class TransformationManagerConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public TransformationManagerConfig() {
		super(ConfigGroups.SERVICE_TRANSFORM, TransformationManager.SERVICE_ID + CONFIG_SUFFIX);
	}
}
