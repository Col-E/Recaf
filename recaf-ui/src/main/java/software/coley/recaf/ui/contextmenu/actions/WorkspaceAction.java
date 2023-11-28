package software.coley.recaf.ui.contextmenu.actions;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.cell.ContextMenuProviderFactory;
import software.coley.recaf.workspace.model.Workspace;

/**
 * For simplifying references to methods in {@link ContextMenuProviderFactory} implementations.
 *
 * @author Matt Coley
 */
public interface WorkspaceAction {
	/**
	 * @param workspace
	 * 		Target workspace.
	 */
	void accept(@Nonnull Workspace workspace);
}
