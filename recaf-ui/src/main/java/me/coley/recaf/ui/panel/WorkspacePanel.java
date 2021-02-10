package me.coley.recaf.ui.panel;

import javafx.scene.layout.BorderPane;
import me.coley.recaf.ControllerListener;
import me.coley.recaf.ui.control.WorkspaceFilterField;
import me.coley.recaf.ui.control.tree.WorkspaceTree;
import me.coley.recaf.workspace.Workspace;

/**
 * Panel representing the current workspace.
 *
 * @author Matt Coley
 */
public class WorkspacePanel extends BorderPane implements ControllerListener {
	private final WorkspaceTree tree = new WorkspaceTree();
	private final WorkspaceFilterField filter = new WorkspaceFilterField(tree);
	private final WorkspaceButtonsPanel buttons = new WorkspaceButtonsPanel(tree);

	/**
	 * Create the panel.
	 */
	public WorkspacePanel() {
		setCenter(tree);
		setBottom(filter);
		// setTop(buttons); // TODO: finish button panel
		// Any typing in the tree should be fed into the filter
		tree.setOnKeyPressed(e -> {
			if (e.getText() != null && !e.getText().isEmpty()) {
				filter.requestFocus();
			}
		});
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

	/**
	 * @return Filter text field.
	 */
	public WorkspaceFilterField getFilter() {
		return filter;
	}

	@Override
	public void onNewWorkspace(Workspace oldWorkspace, Workspace newWorkspace) {
		tree.setWorkspace(newWorkspace);
	}
}
