package me.coley.recaf.ui.panel;

import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.ui.control.tree.WorkspaceTree;

public class WorkspaceButtonsPanel extends BorderPane {
	private final WorkspaceTree tree;
	private final Button hideLibraries = new Button();

	public WorkspaceButtonsPanel(WorkspaceTree tree) {
		this.tree = tree;
	}
}
