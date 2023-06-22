package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.Info;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Icon provider for {@link Bundle bundles}, to be plugged into {@link IconProviderService}
 * to allow for third party icon customization.
 *
 * @author Matt Coley
 */
public interface BundleIconProviderFactory extends IconProviderFactory {
	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		The bundle to create an icon for.
	 *
	 * @return Icon provider for the bundle.
	 */
	@Nonnull
	default IconProvider getBundleIconProvider(@Nonnull Workspace workspace,
											   @Nonnull WorkspaceResource resource,
											   @Nonnull Bundle<? extends Info> bundle) {
		return emptyProvider();
	}
}
