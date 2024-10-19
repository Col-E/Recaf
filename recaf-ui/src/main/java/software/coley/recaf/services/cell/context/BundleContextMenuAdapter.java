package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.info.Info;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Context menu adapter for {@link Bundle} types.
 *
 * @author Matt Coley
 */
public interface BundleContextMenuAdapter extends ContextMenuAdapter {
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
	 * 		The bundle the menu is for.
	 */
	void adaptBundleContextMenu(@Nonnull ContextMenu menu,
	                            @Nonnull ContextSource source,
	                            @Nonnull Workspace workspace,
	                            @Nonnull WorkspaceResource resource,
	                            @Nonnull Bundle<? extends Info> bundle);
}
