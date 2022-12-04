package me.coley.recaf.workspace;

import jakarta.enterprise.context.ApplicationScoped;
import me.coley.recaf.workspace.resource.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager of the current workspace, and delegation of workspace events to listeners.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class WorkspaceManager {
	private final List<WorkspaceOpenListener> openListeners = new ArrayList<>();
	private final List<WorkspaceCloseListener> closeListeners = new ArrayList<>();
	private final List<WorkspaceCloseCondition> closeConditions = new ArrayList<>();
	private Workspace current;

	/**
	 * @param object
	 * 		Listener of potentially multiple types to add.
	 */
	public void addListener(Object object) {
		if (object instanceof WorkspaceOpenListener)
			addWorkspaceOpenListener((WorkspaceOpenListener) object);
		if (object instanceof WorkspaceCloseListener)
			addWorkspaceCloseListener((WorkspaceCloseListener) object);
		if (object instanceof WorkspaceCloseCondition)
			addWorkspaceCloseCondition((WorkspaceCloseCondition) object);
	}

	/**
	 * @param object
	 * 		Listener of potentially multiple types to remove.
	 */
	public void removeListener(Object object) {
		if (object instanceof WorkspaceOpenListener)
			removeWorkspaceOpenListener((WorkspaceOpenListener) object);
		if (object instanceof WorkspaceCloseListener)
			removeWorkspaceCloseListener((WorkspaceCloseListener) object);
		if (object instanceof WorkspaceCloseCondition)
			removeWorkspaceCloseCondition((WorkspaceCloseCondition) object);
	}

	/**
	 * @param listener
	 * 		Listener to call when current workspace is assigned to a new value.
	 */
	public void addWorkspaceOpenListener(WorkspaceOpenListener listener) {
		openListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to no longer call when current workspace is assigned to a new value.
	 *
	 * @return {@code true} on removal. {@code false} if it was not registered prior.
	 */
	public boolean removeWorkspaceOpenListener(WorkspaceOpenListener listener) {
		return openListeners.remove(listener);
	}

	/**
	 * @param listener
	 * 		Listener to call when current workspace is closed.
	 */
	public void addWorkspaceCloseListener(WorkspaceCloseListener listener) {
		closeListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to call when current workspace is closed.
	 *
	 * @return {@code true} on removal. {@code false} if it was not registered prior.
	 */
	public boolean removeWorkspaceCloseListener(WorkspaceCloseListener listener) {
		return closeListeners.remove(listener);
	}

	/**
	 * @param condition
	 * 		Condition to potentially limit when the current workspace is closable.
	 */
	public void addWorkspaceCloseCondition(WorkspaceCloseCondition condition) {
		closeConditions.add(condition);
	}

	/**
	 * @param condition
	 * 		Condition to no longer potentially limit when the current workspace is closable.
	 *
	 * @return {@code true} on removal. {@code false} if it was not registered prior.
	 */
	public boolean removeWorkspaceCloseCondition(WorkspaceCloseCondition condition) {
		return closeConditions.remove(condition);
	}

	/**
	 * @return Current open workspace.
	 */
	public Workspace getCurrent() {
		return current;
	}

	/**
	 * @return Primary resource of the current workspace.
	 */
	public Resource getCurrentPrimaryResource() {
		if (current == null) return null;
		return current.getResources().getPrimary();
	}

	/**
	 * @param current
	 * 		New workspace.
	 *
	 * @return {@code true} if the prior workspace was successfully closed.
	 *
	 * @see #setCurrent(Workspace, boolean)
	 */
	public boolean setCurrent(Workspace current) {
		return setCurrent(current, false);
	}

	/**
	 * @param workspace
	 * 		New workspace.
	 * @param override
	 *        {@code true} to bypass all registered {@link WorkspaceCloseCondition}.
	 *
	 * @return {@code true} if the prior workspace was successfully closed.
	 * Anybody can register a {@link WorkspaceCloseCondition} to prevent closure.
	 * By default, only the UI has a closure condition, which spawns an <i>"Are you sure?"</i> prompt.
	 */
	public boolean setCurrent(Workspace workspace, boolean override) {
		// Close current workspace
		if (current != null) {
			if (override || closeConditions.stream().allMatch(condition -> condition.canClose(current))) {
				current.cleanup();
				closeListeners.forEach(listener -> listener.onWorkspaceClosed(current));
			} else {
				// Some condition prevented workspace closure
				return false;
			}
		}

		// Set new workspace
		current = workspace;

		// Fire listeners if new value is non-null
		if (current != null) openListeners.forEach(listener -> listener.onWorkspaceOpened(current));
		return true;
	}
}
