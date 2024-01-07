package software.coley.recaf.ui.contextmenu.actions;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.cell.ContextMenuProviderFactory;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * For simplifying references to methods in {@link ContextMenuProviderFactory} implementations.
 *
 * @author Matt Coley
 */
public interface ResourceAction {
	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Target resource.
	 */
	void accept(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource);
}
