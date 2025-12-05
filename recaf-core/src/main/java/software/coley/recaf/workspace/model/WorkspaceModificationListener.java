package software.coley.recaf.workspace.model;

import jakarta.annotation.Nonnull;
import software.coley.recaf.behavior.PrioritySortable;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Listener for receiving workspace update events.
 *
 * @author Matt Coley
 */
public interface WorkspaceModificationListener extends PrioritySortable {
	/**
	 * @param workspace
	 * 		The workspace.
	 * @param library
	 * 		Library added to the workspace.
	 */
	void onAddLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library);

	/**
	 * @param workspace
	 * 		The workspace.
	 * @param library
	 * 		Library removed from the workspace.
	 */
	void onRemoveLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library);
}