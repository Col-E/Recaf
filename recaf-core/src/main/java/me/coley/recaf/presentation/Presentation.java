package me.coley.recaf.presentation;

import me.coley.recaf.Controller;
import me.coley.recaf.workspace.WorkspaceCloseCondition;
import me.coley.recaf.workspace.WorkspaceCloseListener;
import me.coley.recaf.workspace.WorkspaceModificationListener;
import me.coley.recaf.workspace.WorkspaceOpenListener;
import me.coley.recaf.workspace.resource.ResourceClassListener;
import me.coley.recaf.workspace.resource.ResourceDexClassListener;
import me.coley.recaf.workspace.resource.ResourceFileListener;

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
	interface WorkspacePresentation extends WorkspaceOpenListener, WorkspaceCloseListener, WorkspaceCloseCondition,
			WorkspaceModificationListener, ResourceClassListener, ResourceDexClassListener, ResourceFileListener {
	}
}
