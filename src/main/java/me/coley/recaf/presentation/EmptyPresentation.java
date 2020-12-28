package me.coley.recaf.presentation;

import me.coley.recaf.Controller;
import me.coley.recaf.workspace.Workspace;

/**
 * A presentation that has no display at all. Ideal for using Recaf as a library.
 *
 * @author Matt Coley
 */
public class EmptyPresentation implements Presentation {
	@Override
	public void initialize(Controller controller) {
		// no-op
	}

	@Override
	public WorkspacePresentation workspaceLayer() {
		return new EmptyWorkspacePresentation();
	}

	private static class EmptyWorkspacePresentation implements WorkspacePresentation {
		@Override
		public boolean closeWorkspace(Workspace workspace) {
			return true;
		}

		@Override
		public void openWorkspace(Workspace workspace) {
			// no-op
		}
	}
}
