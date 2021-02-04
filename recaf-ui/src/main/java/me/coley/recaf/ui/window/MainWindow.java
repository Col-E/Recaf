package me.coley.recaf.ui.window;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.ui.panel.WorkspacePanel;

/**
 * Main window for Recaf.
 *
 * @author Matt Coley
 */
public class MainWindow extends WindowBase {
	private final WorkspacePanel workspacePanel = new WorkspacePanel();

	/**
	 * Create the window.
	 */
	public MainWindow() {
		setScene(createScene());
	}

	@Override
	protected Scene createScene() {
		BorderPane pane = new BorderPane();
		pane.setCenter(workspacePanel);
		return new Scene(pane);
	}

	/**
	 * @return Panel representing the current workspace.
	 */
	public WorkspacePanel getWorkspacePanel() {
		return workspacePanel;
	}
}
