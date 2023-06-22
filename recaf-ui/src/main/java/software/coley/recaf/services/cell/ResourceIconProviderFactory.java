package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Icon provider for {@link WorkspaceResource resources}, to be plugged into {@link IconProviderService}
 * to allow for third party icon customization.
 *
 * @author Matt Coley
 */
public interface ResourceIconProviderFactory extends IconProviderFactory {
	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		The resource to create an icon for.
	 *
	 * @return Icon provider for the resource.
	 */
	@Nonnull
	default IconProvider getResourceIconProvider(@Nonnull Workspace workspace,
												 @Nonnull WorkspaceResource resource) {
		return emptyProvider();
	}
}
