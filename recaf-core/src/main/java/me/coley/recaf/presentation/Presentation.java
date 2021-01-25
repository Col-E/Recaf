package me.coley.recaf.presentation;

import me.coley.recaf.Controller;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.ClassInfo;
import me.coley.recaf.workspace.resource.FileInfo;
import me.coley.recaf.workspace.resource.Resource;

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
	interface WorkspacePresentation {
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

		/**
		 * Called when a new class is added.
		 *
		 * @param resource
		 * 		Resource affected.
		 * @param newValue
		 * 		Class added to the resource.
		 */
		void onNewClass(Resource resource, ClassInfo newValue);

		/**
		 * Called when the old class is replaced by the new class.
		 *
		 * @param resource
		 * 		Resource affected.
		 * @param oldValue
		 * 		Prior class value.
		 * @param newValue
		 * 		New class value.
		 */
		void onUpdateClass(Resource resource, ClassInfo oldValue, ClassInfo newValue);

		/**
		 * Called when an old class is removed.
		 *
		 * @param resource
		 * 		Resource affected.
		 * @param oldValue
		 * 		Class removed to the resource.
		 */
		void onRemoveClass(Resource resource, ClassInfo oldValue);

		/**
		 * Called when a new file is added.
		 *
		 * @param resource
		 * 		Resource affected.
		 * @param newValue
		 * 		File added to the resource.
		 */
		void onNewFile(Resource resource, FileInfo newValue);

		/**
		 * Called when the old file is replaced by the new file.
		 *
		 * @param resource
		 * 		Resource affected.
		 * @param oldValue
		 * 		Prior file value.
		 * @param newValue
		 * 		New file value.
		 */
		void onUpdateFile(Resource resource, FileInfo oldValue, FileInfo newValue);

		/**
		 * Called when an old file is removed.
		 *
		 * @param resource
		 * 		Resource affected.
		 * @param oldValue
		 * 		File removed to the resource.
		 */
		void onRemoveFile(Resource resource, FileInfo oldValue);
	}
}
