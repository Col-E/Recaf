package me.coley.recaf.ui.panel;

import javafx.scene.layout.BorderPane;
import me.coley.recaf.ControllerListener;
import me.coley.recaf.ui.control.tree.WorkspaceTree;
import me.coley.recaf.workspace.Workspace;

/**
 * Panel representing the current workspace.
 *
 * @author Matt Coley
 */
public class WorkspacePanel extends BorderPane implements ControllerListener {
	private final WorkspaceTree tree = new WorkspaceTree();

	/**
	 * Create the panel.
	 */
	public WorkspacePanel() {
		// TODO: Top: Button row of quick workspace actions + "..." menu for more options
		setCenter(tree);
	}

	/**
	 * @return Current workspace.
	 */
	public Workspace getWorkspace() {
		return tree.getWorkspace();
	}

	/**
	 * @return Tree representation of {@link #getWorkspace() current workspace}.
	 */
	public WorkspaceTree getTree() {
		return tree;
	}

	@Override
	public void onNewWorkspace(Workspace oldWorkspace, Workspace newWorkspace) {
		tree.setWorkspace(newWorkspace);
	}
}
