package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Context menu adapter for directories <i>(paths in {@link FileBundle})</i>.
 *
 * @author Matt Coley
 */
public interface DirectoryContextMenuAdapter extends ContextMenuAdapter {
	/**
	 * @param menu
	 * 		The menu to adapt.
	 * @param source
	 * 		Context request origin.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param directoryName
	 * 		The full path of the directory the menu is for.
	 */
	void adaptDirectoryContextMenu(@Nonnull ContextMenu menu,
	                               @Nonnull ContextSource source,
	                               @Nonnull Workspace workspace,
	                               @Nonnull WorkspaceResource resource,
	                               @Nonnull FileBundle bundle,
	                               @Nonnull String directoryName);
}
