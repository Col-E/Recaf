package software.coley.recaf;

import jakarta.annotation.Nonnull;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.cdi.UiInitializationEvent;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.ui.RecafTheme;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.docking.DockingManager;
import software.coley.recaf.ui.docking.DockingRegion;
import software.coley.recaf.ui.docking.DockingTab;
import software.coley.recaf.ui.menubar.MainMenu;
import software.coley.recaf.ui.pane.LoggingPane;
import software.coley.recaf.ui.pane.WelcomePane;
import software.coley.recaf.ui.pane.WorkspaceRootPane;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.WorkspaceCloseListener;
import software.coley.recaf.workspace.WorkspaceManager;
import software.coley.recaf.workspace.WorkspaceOpenListener;
import software.coley.recaf.workspace.model.Workspace;

/**
 * JavaFX application entry point.
 *
 * @author Matt Coley
 */
public class RecafApplication extends Application implements WorkspaceOpenListener, WorkspaceCloseListener {
	private final Recaf recaf = Bootstrap.get();
	private final BorderPane root = new BorderPane();

	@Override
	public void start(Stage stage) {
		// Setup global style
		setUserAgentStylesheet(new RecafTheme().getUserAgentStylesheet());

		// Get components
		MainMenu menu = recaf.get(MainMenu.class);
		WelcomePane pane = recaf.get(WelcomePane.class);
		Node logging = createLoggingWrapper();

		// Layout
		SplitPane splitPane = new SplitPane(root, logging);
		SplitPane.setResizableWithParent(logging, false);
		splitPane.setOrientation(Orientation.VERTICAL);
		splitPane.setDividerPositions(0.21); // Behaves inverse to expectation in these specific circumstances
		BorderPane wrapper = new BorderPane();
		wrapper.setTop(menu);
		wrapper.setCenter(splitPane);
		wrapper.getStyleClass().addAll("padded", "bg-inset");
		root.setCenter(pane);

		// Register listener
		WorkspaceManager workspaceManager = recaf.get(WorkspaceManager.class);
		workspaceManager.addWorkspaceOpenListener(this);
		workspaceManager.addWorkspaceCloseListener(this);

		// Display
		stage.setMinWidth(900);
		stage.setMinHeight(600);
		Scene value = new RecafScene(wrapper);
		stage.setScene(value);
		stage.getIcons().add(Icons.getImage(Icons.LOGO));
		stage.setTitle("Recaf");
		stage.show();

		// Register main window
		WindowManager windowManager = recaf.get(WindowManager.class);
		windowManager.register(WindowManager.WIN_MAIN, stage);

		// Publish UI init event
		recaf.getContainer().getBeanContainer().getEvent().fire(new UiInitializationEvent());
	}

	private Node createLoggingWrapper() {
		LoggingPane logging = recaf.get(LoggingPane.class);
		DockingRegion dockingPane = recaf.get(DockingManager.class).newRegion();
		DockingTab tab = dockingPane.createTab(Lang.getBinding("logging.title"), logging);
		tab.setGraphic(new FontIconView(CarbonIcons.TERMINAL));
		tab.setClosable(false);
		return dockingPane;
	}

	@Override
	public void onWorkspaceClosed(@Nonnull Workspace workspace) {
		FxThreadUtil.run(() -> {
			WelcomePane pane = recaf.get(WelcomePane.class);
			root.setCenter(pane);
		});
	}

	@Override
	public void onWorkspaceOpened(@Nonnull Workspace workspace) {
		FxThreadUtil.run(() -> {
			WorkspaceRootPane pane = recaf.get(WorkspaceRootPane.class);
			root.setCenter(pane);
		});
	}
}
