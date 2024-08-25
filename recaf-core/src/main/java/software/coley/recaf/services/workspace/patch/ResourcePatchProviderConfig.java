package software.coley.recaf.services.workspace.patch;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link PatchProvider}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ResourcePatchProviderConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public ResourcePatchProviderConfig() {
		super(ConfigGroups.SERVICE_IO, PatchProvider.SERVICE_ID + CONFIG_SUFFIX);
	}
}
