package software.coley.recaf.services.vm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link VirtualInvoker}
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class VirtualInvokerConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public VirtualInvokerConfig() {
		super(ConfigGroups.SERVICE_DEOBFUSCATION, VirtualInvoker.SERVICE_ID + CONFIG_SUFFIX);
	}
}