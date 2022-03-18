package me.coley.recaf.ui.window;

import com.panemu.tiwulfx.control.dock.DetachableTabPane;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.WindowEvent;
import me.coley.recaf.BuildConfig;
import me.coley.recaf.RecafUI;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.control.LoggingTextArea;
import me.coley.recaf.ui.docking.DockTab;
import me.coley.recaf.ui.docking.DockingRegion;
import me.coley.recaf.ui.docking.RecafDockingManager;
import me.coley.recaf.ui.pane.WelcomePane;
import me.coley.recaf.ui.pane.WorkspacePane;
import me.coley.recaf.ui.prompt.WorkspaceClosePrompt;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.workspace.Workspace;

/**
 * Main window for Recaf.
 *
 * @author Matt Coley
 */
public class MainWindow extends WindowBase {
	private final WorkspacePane workspacePane = new WorkspacePane();

	/**
	 * Create the window.
	 */
	public MainWindow() {
		init();
		setTitle("Recaf " + BuildConfig.VERSION);
	}

	@Override
	protected void init() {
		super.init();
		// Let users cancel closing the window if they have prompting enabled
		addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
			// Close if warning prompting is disabled
			if (!Configs.display().promptCloseWorkspace) {
				System.exit(0);
				return;
			}
			// Close if there is no workspace
			Workspace workspace = RecafUI.getController().getWorkspace();
			if (workspace == null) {
				System.exit(0);
				return;
			}
			// Close if the user is OK with closing.
			// Otherwise cancel the close request and keep Recaf open
			if (WorkspaceClosePrompt.prompt(workspace)) {
				System.exit(0);
			} else {
				event.consume();
			}
		});
	}

	@Override
	protected Scene createScene() {
		RecafDockingManager docking = RecafDockingManager.getInstance();

		// Create regions for docking
		DockingRegion region0Workspace = docking.createRegion();
		DockingRegion region1Welcome = docking.createRegion();
		DockingRegion region2Logging = docking.createRegion();

		// Create tab content
		DockTab workspaceTab =  new DockTab(Lang.getBinding("workspace.title"), workspacePane);
		DockTab loggingTab = new DockTab(Lang.getBinding("logging.title"), LoggingTextArea.getInstance());
		workspaceTab.setClosable(false);
		loggingTab.setClosable(false);

		// Populate regions with tabs
		docking.createTabIn(region0Workspace, () -> workspaceTab);
		docking.createTabIn(region1Welcome, () -> new DockTab(Lang.getBinding("welcome.title"),  new WelcomePane()));
		docking.createTabIn(region2Logging, () -> loggingTab);

		// Create splits for docking regions
		SplitPane horizontalSplit = new SplitPane(region0Workspace, region1Welcome);
		SplitPane verticalSplit = new SplitPane(horizontalSplit, region2Logging);
		horizontalSplit.setDividerPosition(0, 0.30);
		verticalSplit.setDividerPosition(0, 0.76);
		verticalSplit.setOrientation(Orientation.VERTICAL);

		// Remove workspace/logging from history so new files open in the larger 'region 1'
		docking.removeInteractionHistory(region0Workspace);
		docking.removeInteractionHistory(region2Logging);

		// Wrap it up, add menu to top
		BorderPane root = new BorderPane(verticalSplit);
		root.setTop(MainMenu.getInstance());
		return new Scene(root);
	}

	/**
	 * @return Panel representing the current workspace.
	 */
	public WorkspacePane getWorkspacePane() {
		return workspacePane;
	}
}
