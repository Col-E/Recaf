package me.coley.recaf.ui.window;

import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.ui.control.LoggingTextArea;
import me.coley.recaf.ui.control.DockableTab;
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
		init();
	}

	@Override
	protected Scene createScene() {
		// Content
		DockableTab workspaceWrapper = DockableTab.locked("Workspace", workspacePanel);
		DockableTab contentWrapper = DockableTab.locked("Content", new BorderPane()); // TODO: Filler content
		DockableTab loggingWrapper = DockableTab.locked("Logging", LoggingTextArea.getInstance());
		// Setup layout
		SplitPane vertical = new SplitPane();
		SplitPane horizontal = new SplitPane();
		horizontal.getItems().addAll(workspaceWrapper, contentWrapper);
		horizontal.setDividerPositions(0.33);
		vertical.setDividerPositions(0.76);
		vertical.setOrientation(Orientation.VERTICAL);
		vertical.getItems().addAll(horizontal, loggingWrapper);
		vertical.setPrefWidth(1080);
		// TODO: Make it so the workspace panel does not scale when dropped into a new location
		SplitPane.setResizableWithParent(workspaceWrapper, Boolean.FALSE);
		SplitPane.setResizableWithParent(horizontal, Boolean.FALSE);
		return new Scene(vertical);
	}

	/**
	 * @return Panel representing the current workspace.
	 */
	public WorkspacePanel getWorkspacePanel() {
		return workspacePanel;
	}
}
