package me.coley.recaf.ui.panel;

import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.ControllerListener;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.control.WorkspaceFilterField;
import me.coley.recaf.ui.control.tree.WorkspaceTreeWrapper;
import me.coley.recaf.workspace.Workspace;

/**
 * Panel representing the current workspace.
 *
 * @author Matt Coley
 */
public class WorkspacePanel extends BorderPane implements ControllerListener {
	private final WorkspaceTreeWrapper tree = new WorkspaceTreeWrapper();
	private final WorkspaceFilterField filter = new WorkspaceFilterField(tree);
	private final WorkspaceButtonsPanel buttons = new WorkspaceButtonsPanel(tree);

	/**
	 * Create the panel.
	 */
	public WorkspacePanel() {
		// Wrap bottom non-tree elements
		BorderPane bottomWrapper = new BorderPane();
		bottomWrapper.setCenter(filter);
		if (Configs.display().showFilterButtons)
			bottomWrapper.setTop(buttons);
		// Set content
		setCenter(tree);
		setBottom(bottomWrapper);
		// Any typing in the tree should be fed into the filter
		tree.setOnKeyPressed(e -> {
			if (e.getText() != null && !e.getText().isEmpty()) {
				filter.requestFocus();
			} else if (e.getCode() == KeyCode.ESCAPE) {
				filter.clear();
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
	public WorkspaceTreeWrapper getTree() {
		return tree;
	}

	/**
	 * @return Filter text field.
	 */
	public WorkspaceFilterField getFilter() {
		return filter;
	}

	/**
	 * @return Button panel.
	 */
	public WorkspaceButtonsPanel getButtons() {
		return buttons;
	}

	@Override
	public void onNewWorkspace(Workspace oldWorkspace, Workspace newWorkspace) {
		tree.setWorkspace(newWorkspace);
	}
}
