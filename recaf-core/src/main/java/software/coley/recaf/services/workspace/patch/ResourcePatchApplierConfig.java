package software.coley.recaf.services.workspace.patch;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link PatchApplier}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ResourcePatchApplierConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public ResourcePatchApplierConfig() {
		super(ConfigGroups.SERVICE_IO, PatchApplier.SERVICE_ID + CONFIG_SUFFIX);
	}
}
