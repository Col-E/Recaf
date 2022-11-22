package me.coley.recaf.cdi;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import me.coley.recaf.workspace.Workspace;

/**
 * Common place for dispatching 'events' to CDI internals.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class Dispatch {
	private static final WorkspaceBeanContext workspaceBeanContext = WorkspaceBeanContext.getInstance();

	public static void workspaceOpen(@Nonnull Workspace workspace) {
		workspaceBeanContext.onWorkspaceOpened(workspace);
	}

	public static void workspaceClose(@Nonnull Workspace workspace) {
		workspaceBeanContext.onWorkspaceOpened(workspace);
	}
}
