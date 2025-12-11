package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link TransformationApplierService}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class TransformationApplierConfig extends BasicConfigContainer implements ServiceConfig {
	private final ObservableBoolean parallelize = new ObservableBoolean(true);

	@Inject
	public TransformationApplierConfig() {
		super(ConfigGroups.SERVICE_TRANSFORM, TransformationApplierService.SERVICE_ID + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("parallelize", boolean.class, parallelize));
	}

	/**
	 * @return {@code true} to enable parallelization of transformer applications.
	 */
	@Nonnull
	public ObservableBoolean doParallelize() {
		return parallelize;
	}
}
