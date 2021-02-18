package me.coley.recaf.presentation;

import me.coley.recaf.Controller;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.*;

/**
 * Base outline for how Recaf should present behaviors.
 *
 * @author Matt Coley
 */
public interface Presentation {
	/**
	 * Setup the presentation layer.
	 *
	 * @param controller
	 * 		Parent controller the presentation layer represents.
	 */
	void initialize(Controller controller);

	/**
	 * @return Workspace presentation layer.
	 */
	WorkspacePresentation workspaceLayer();

	/**
	 * Presentation implementation for workspace content.
	 */
	interface WorkspacePresentation extends ResourceClassListener, ResourceDexClassListener, ResourceFileListener {
		/**
		 * Close the given <i>(should match current)</i> workspace.
		 *
		 * @param workspace
		 * 		Closed workspace.
		 *
		 * @return {@code true} on successful close. {@code false} if the closure was cancelled.
		 */
		boolean closeWorkspace(Workspace workspace);

		/**
		 * Opens the given <i>(should match current)</i> workspace.
		 *
		 * @param workspace
		 * 		New workspace.
		 */
		void openWorkspace(Workspace workspace);
	}
}
