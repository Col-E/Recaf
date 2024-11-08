package software.coley.recaf.services.transform;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link TransformationApplier}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class TransformationApplierConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public TransformationApplierConfig() {
		super(ConfigGroups.SERVICE_TRANSFORM, TransformationApplier.SERVICE_ID + CONFIG_SUFFIX);
	}
}
