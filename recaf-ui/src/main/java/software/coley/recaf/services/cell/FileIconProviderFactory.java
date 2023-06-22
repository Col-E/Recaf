package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Icon provider for {@link FileInfo files}, to be plugged into {@link IconProviderService}
 * to allow for third party icon customization.
 *
 * @author Matt Coley
 */
public interface FileIconProviderFactory extends IconProviderFactory {
	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		The file to create an icon for.
	 *
	 * @return Icon provider for the file.
	 */
	@Nonnull
	default IconProvider getFileInfoIconProvider(@Nonnull Workspace workspace,
												 @Nonnull WorkspaceResource resource,
												 @Nonnull FileBundle bundle,
												 @Nonnull FileInfo info) {
		return emptyProvider();
	}
}
