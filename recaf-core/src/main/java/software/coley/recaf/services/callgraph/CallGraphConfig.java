package software.coley.recaf.services.callgraph;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link CallGraph}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class CallGraphConfig extends BasicConfigContainer implements ServiceConfig {
	private final ObservableBoolean active = new ObservableBoolean(true);

	@Inject
	public CallGraphConfig() {
		super(ConfigGroups.SERVICE_ANALYSIS, CallGraph.SERVICE_ID + CONFIG_SUFFIX);
		// Add values
		addValue(new BasicConfigValue<>("active", boolean.class, active));
	}

	/**
	 * @return Active state of call graph service.
	 */
	@Nonnull
	public ObservableBoolean getActive() {
		return active;
	}
}
