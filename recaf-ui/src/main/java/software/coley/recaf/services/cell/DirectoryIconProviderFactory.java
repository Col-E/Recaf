package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Icon provider for directories <i>(paths in {@link FileBundle})</i>, to be plugged into {@link IconProviderService}
 * to allow for third party icon customization.
 *
 * @author Matt Coley
 */
public interface DirectoryIconProviderFactory extends IconProviderFactory {
	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param directoryName
	 * 		The full path of the directory.
	 *
	 * @return Icon provider for the directory.
	 */
	@Nonnull
	default IconProvider getDirectoryIconProvider(@Nonnull Workspace workspace,
												  @Nonnull WorkspaceResource resource,
												  @Nonnull FileBundle bundle,
												  @Nonnull String directoryName) {
		return emptyProvider();
	}
}
