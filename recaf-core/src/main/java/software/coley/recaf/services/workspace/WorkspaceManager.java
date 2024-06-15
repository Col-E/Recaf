package software.coley.recaf.services.workspace;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import software.coley.recaf.services.Service;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.WorkspaceModificationListener;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collections;
import java.util.List;

/**
 * Service outline for managing workspace importing/exporting and the currently open workspace.
 *
 * @author Matt Coley
 */
public interface WorkspaceManager extends Service {
	String SERVICE_ID = "workspace-manager";

	/**
	 * Exposes the current workspace directly and through CDI.
	 * Any {@link Instance<Workspace>} in the Recaf application should point to this value.
	 *
	 * @return The current active workspace. May be {@code null} when no active workspace is open.
	 */
	@Nullable
	@Produces
	@Dependent
	Workspace getCurrent();

	/**
	 * @param workspace
	 * 		New workspace to set as the active workspace.
	 *
	 * @return {@code true} when the workspace assignment is a success.
	 * {@code false} if the assignment was blocked for some reason.
	 */
	default boolean setCurrent(@Nullable Workspace workspace) {
		Workspace current = getCurrent();
		if (current == null) {
			// If there is no current workspace, then just assign it.
			setCurrentIgnoringConditions(workspace);
			return true;
		} else if (getWorkspaceCloseConditions().stream()
				.allMatch(condition -> condition.canClose(current))) {
			// Otherwise, check if the conditions allow for closing the prior workspace.
			// If so, then assign the new workspace.
			setCurrentIgnoringConditions(workspace);
			return true;
		}
		// Workspace closure conditions not met, assignment denied.
		return false;
	}

	/**
	 * Effectively {@link #setCurrent(Workspace)} except any blocking conditions are bypassed.
	 * <br>
	 * Listeners for open/close events must be called when implementing this.
	 *
	 * @param workspace
	 * 		New workspace to set as the active workspace.
	 */
	void setCurrentIgnoringConditions(@Nullable Workspace workspace);

	/**
	 * Closes the current workspace.
	 *
	 * @return {@code true} on success.
	 */
	default boolean closeCurrent() {
		if (getCurrent() != null)
			return setCurrent(null);
		return true;
	}

	/**
	 * @param primary
	 * 		Primary resource for editing.
	 *
	 * @return New workspace of resource.
	 */
	@Nonnull
	default Workspace createWorkspace(@Nonnull WorkspaceResource primary) {
		return createWorkspace(primary, Collections.emptyList());
	}

	/**
	 * @param primary
	 * 		Primary resource for editing.
	 * @param libraries
	 * 		Supporting resources.
	 *
	 * @return New workspace of resources
	 */
	@Nonnull
	default Workspace createWorkspace(@Nonnull WorkspaceResource primary, @Nonnull List<WorkspaceResource> libraries) {
		return new BasicWorkspace(primary, libraries);
	}

	/**
	 * @return Conditions in the manager that can prevent {@link #setCurrent(Workspace)} from going through.
	 */
	@Nonnull
	List<WorkspaceCloseCondition> getWorkspaceCloseConditions();

	/**
	 * @param condition
	 * 		Condition to add.
	 */
	void addWorkspaceCloseCondition(@Nonnull WorkspaceCloseCondition condition);

	/**
	 * @param condition
	 * 		Condition to remove.
	 */
	void removeWorkspaceCloseCondition(@Nonnull WorkspaceCloseCondition condition);

	/**
	 * @return Listeners for when a new workspace is assigned as the current one.
	 */
	@Nonnull
	List<WorkspaceOpenListener> getWorkspaceOpenListeners();

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	void addWorkspaceOpenListener(@Nonnull WorkspaceOpenListener listener);

	/**
	 * @param listener
	 * 		Listener to remove.
	 */
	void removeWorkspaceOpenListener(@Nonnull WorkspaceOpenListener listener);

	/**
	 * @return Listeners for when the current workspace is removed as being current.
	 */
	@Nonnull
	List<WorkspaceCloseListener> getWorkspaceCloseListeners();

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	void addWorkspaceCloseListener(@Nonnull WorkspaceCloseListener listener);

	/**
	 * @param listener
	 * 		Listener to remove.
	 */
	void removeWorkspaceCloseListener(@Nonnull WorkspaceCloseListener listener);

	/**
	 * @return Listeners to add to any workspace passed to {@link #setCurrent(Workspace)}.
	 */
	@Nonnull
	List<WorkspaceModificationListener> getDefaultWorkspaceModificationListeners();

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	void addDefaultWorkspaceModificationListeners(@Nonnull WorkspaceModificationListener listener);

	/**
	 * @param listener
	 * 		Listener to remove.
	 */
	void removeDefaultWorkspaceModificationListeners(@Nonnull WorkspaceModificationListener listener);
}
