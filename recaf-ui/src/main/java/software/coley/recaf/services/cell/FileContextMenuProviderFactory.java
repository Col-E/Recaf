package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Context menu provider for {@link FileInfo types}, to be plugged into {@link ContextMenuProviderService}
 * to allow for third party menu customization.
 *
 * @author Matt Coley
 */
public interface FileContextMenuProviderFactory extends ContextMenuProviderFactory {
	/**
	 * @param source
	 * 		Context request origin.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		The file to create a menu for.
	 *
	 * @return Menu provider for the file.
	 */
	@Nonnull
	default ContextMenuProvider getFileInfoContextMenuProvider(@Nonnull ContextSource source,
															   @Nonnull Workspace workspace,
															   @Nonnull WorkspaceResource resource,
															   @Nonnull FileBundle bundle,
															   @Nonnull FileInfo info) {
		return emptyProvider();
	}
}
