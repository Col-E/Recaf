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
import me.coley.recaf.ui.pane.DockingRootPane;
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
	private final DockingRootPane dockingRootPane = new DockingRootPane();

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
		// Content
		SplitPane initialSplit;
		dockingRootPane.setPrefWidth(1080);
		dockingRootPane.createLockedTab("workspace.title", Lang.getBinding("workspace.title"), workspacePane);
		initialSplit = dockingRootPane.createNewSplit(Orientation.HORIZONTAL, 0.30);
		dockingRootPane.createTab("welcome.title", Lang.getBinding("welcome.title"), new WelcomePane());
		dockingRootPane.createNewSplit(Orientation.VERTICAL, 0.76);
		dockingRootPane.createLockedTab("logging.title", Lang.getBinding("logging.title"), LoggingTextArea.getInstance());
		// Mark main content region for new tabs
		DetachableTabPane contentWrapper = (DetachableTabPane) initialSplit.getItems().get(1);
		dockingRootPane.pushRecentTabPane(contentWrapper);
		BorderPane root = new BorderPane(dockingRootPane);
		root.setTop(MainMenu.getInstance());
		return new Scene(root);
	}

	/**
	 * @return Docking panel.
	 */
	public DockingRootPane getDockingRootPane() {
		return dockingRootPane;
	}

	/**
	 * @return Panel representing the current workspace.
	 */
	public WorkspacePane getWorkspacePane() {
		return workspacePane;
	}
}
