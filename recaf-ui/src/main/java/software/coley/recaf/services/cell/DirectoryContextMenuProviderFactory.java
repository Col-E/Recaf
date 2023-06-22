package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Context menu provider for directories <i>(paths in {@link FileBundle})</i>,
 * to be plugged into {@link ContextMenuProviderService} to allow for third party menu customization.
 *
 * @author Matt Coley
 */
public interface DirectoryContextMenuProviderFactory extends ContextMenuProviderFactory {
	/**
	 * @param source
	 * 		Context request origin.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param directoryName
	 * 		The full path of the directory.
	 *
	 * @return Menu provider for the directory.
	 */
	@Nonnull
	default ContextMenuProvider getDirectoryContextMenuProvider(@Nonnull ContextSource source,
																@Nonnull Workspace workspace,
																@Nonnull WorkspaceResource resource,
																@Nonnull FileBundle bundle,
																@Nonnull String directoryName) {
		return emptyProvider();
	}
}
