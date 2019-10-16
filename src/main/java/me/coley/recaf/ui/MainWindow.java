package me.coley.recaf.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import me.coley.recaf.Recaf;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.WorkspaceNavigator;

import java.lang.management.ManagementFactory;
import java.lang.management.PlatformLoggingMXBean;

import static me.coley.recaf.util.ClasspathUtil.resource;

/**
 * Primary window.
 *
 * @author Matt
 */
public class MainWindow extends Application {
	private static MainWindow window;
	private GuiController controller;
	private Stage stage;
	private BorderPane root;
	private MainMenu menubar;

	@Override
	public void start(Stage stage) throws Exception {
		// Set instances
		window = this;
		this.stage = stage;
	}

	private void setup() {
		stage.getIcons().add(new Image(resource("icons/logo.png")));
		stage.setTitle("Recaf");
		root = new BorderPane();
		// Testing out the navigator
		root.setCenter(new WorkspaceNavigator(controller.getWorkspace()));
		Recaf.getWorkspaceSetListeners().add(w -> root.setCenter(new WorkspaceNavigator(w)));
		// Content
		setupMenuBar();
		setupFileTree();
		setupViewport();
		// Create scene & display the window
		Scene scene = new Scene(root, 800, 600);
		controller.windows().reapplyStyle(scene);
		stage.setScene(scene);
	}

	private void setupMenuBar() {
		menubar = new MainMenu(controller);
		root.setTop(menubar);
	}

	private void setupFileTree() {
		// File-tree
		// - search bar @ bottom (Keybind: Find) to show
	}

	private void setupViewport() {
		// View-port
		// - Text view
		//   - For text-files in the workspace
		// - Decompiler view
		//   - works like a decompiler
		// - Edit view
		//   - more like how Recaf was in 1.X
	}

	/**
	 * @return Initial stage.
	 */
	public Stage getStage() {
		return stage;
	}

	/**
	 * @param controller
	 * 		Window context.
	 *
	 * @return main window instance.
	 */
	public static MainWindow get(GuiController controller) {
		if(window == null) {
			// Thread the launch so it doesn't hang the main thread.
			// - Ugly, but we want to pass the controller context before doing any setup
			new Thread(Application::launch).start();
			while(window == null)
				try {
					Thread.sleep(50);
				} catch(Exception ex) { /* ignored */ }
			// Set the controller then run the setup
			window.controller = controller;
			Platform.runLater(() -> {
				window.setup();
				window.stage.show();
				// Disable CSS logger, it complains a lot about non-issues
				ManagementFactory.getPlatformMXBean(PlatformLoggingMXBean.class)
						.setLoggerLevel("javafx.css", "OFF");
			});
		}
		return window;
	}
}
