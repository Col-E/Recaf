package software.coley.recaf;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import software.coley.recaf.cdi.UiInitializationEvent;
import software.coley.recaf.services.navigation.NavigationManager;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.RecafTheme;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.config.WindowScaleConfig;
import software.coley.recaf.ui.menubar.MainMenu;
import software.coley.recaf.ui.pane.LoggingPane;
import software.coley.recaf.ui.docking.DockingLayoutManager;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Icons;
import software.coley.recaf.workspace.PathExportingManager;

/**
 * JavaFX application entry point.
 *
 * @author Matt Coley
 */
public class RecafApplication extends Application {
	private final Recaf recaf = Bootstrap.get();

	@Override
	public void start(Stage stage) {
		// Notify the thread util that FX has been initialized
		FxThreadUtil.onInitialize();

		// Setup global style
		setUserAgentStylesheet(new RecafTheme().getUserAgentStylesheet());

		// Initialize the navigation manager before we get the services below.
		// We want it to get a head start on initalization.
		recaf.get(NavigationManager.class).requestFocus();

		// Get services
		DockingLayoutManager dockingLayoutManager = recaf.get(DockingLayoutManager.class);
		KeybindingConfig keybindingConfig = recaf.get(KeybindingConfig.class);
		WindowManager windowManager = recaf.get(WindowManager.class);
		WorkspaceManager workspaceManager = recaf.get(WorkspaceManager.class);

		// Get components
		MainMenu menu = recaf.get(MainMenu.class);
		LoggingPane logging = recaf.get(LoggingPane.class);

		// Layout
		BorderPane wrapper = new BorderPane();
		wrapper.setTop(menu);
		wrapper.setCenter(dockingLayoutManager.getRoot().asRegion());
		wrapper.getStyleClass().addAll("padded", "bg-inset");

		// Display
		WindowScaleConfig scaleConfig = recaf.get(WindowScaleConfig.class);
		Scene scene = new RecafScene(wrapper);
		scene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
			// Global keybind handling
			if (keybindingConfig.getQuickNav().match(event)) {
				Stage quickNav = windowManager.getQuickNav();
				quickNav.show();
				quickNav.requestFocus();
			} else if (keybindingConfig.getExport().match(event) && workspaceManager.hasCurrentWorkspace()) {
				recaf.get(PathExportingManager.class).export(workspaceManager.getCurrent());
			}
		});
		stage.setMinWidth(450 / scaleConfig.getScale());
		stage.setMinHeight(200 / scaleConfig.getScale());
		stage.setWidth(900 / scaleConfig.getScale());
		stage.setHeight(620 / scaleConfig.getScale());
		stage.setScene(scene);
		stage.getIcons().add(Icons.getImage(Icons.LOGO));
		stage.setTitle("Recaf");
		stage.setOnCloseRequest(e -> ExitDebugLoggingHook.exit(0));
		stage.show();

		// Register main window
		windowManager.register(WindowManager.WIN_MAIN, stage);

		// Publish UI init event
		recaf.getContainer().getBeanContainer().getEvent().fire(new UiInitializationEvent());
	}

}
