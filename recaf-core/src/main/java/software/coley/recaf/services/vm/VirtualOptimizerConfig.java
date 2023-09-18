package software.coley.recaf.services.vm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link VirtualOptimizer}
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class VirtualOptimizerConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public VirtualOptimizerConfig() {
		super(ConfigGroups.SERVICE_DEOBFUSCATION, VirtualOptimizer.SERVICE_ID + CONFIG_SUFFIX);
	}
}