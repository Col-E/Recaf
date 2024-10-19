package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Context menu adapter for {@link WorkspaceResource} types.
 *
 * @author Matt Coley
 */
public interface ResourceContextMenuAdapter extends ContextMenuAdapter {
	/**
	 * @param menu
	 * 		The menu to adapt.
	 * @param source
	 * 		Context request origin.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		The resource the menu is for.
	 */
	void adaptResourceContextMenu(@Nonnull ContextMenu menu,
	                              @Nonnull ContextSource source,
	                              @Nonnull Workspace workspace,
	                              @Nonnull WorkspaceResource resource);
}
