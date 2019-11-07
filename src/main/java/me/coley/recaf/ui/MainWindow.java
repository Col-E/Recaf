package me.coley.recaf.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import me.coley.recaf.Recaf;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.*;
import me.coley.recaf.workspace.JavaResource;

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
	private BorderPane navRoot;
	private BorderPane viewRoot;
	private MainMenu menubar;
	private ViewportTabs tabs;

	@Override
	public void start(Stage stage) throws Exception {
		// Set instances
		window = this;
		this.stage = stage;
	}

	private void setup() {
		stage.getIcons().add(new Image(resource("icons/logo.png")));
		stage.setTitle("Recaf");
		menubar = new MainMenu(controller);
		root = new BorderPane();
		root.setTop(menubar);
		navRoot = new BorderPane();
		viewRoot = new BorderPane();
		tabs = new ViewportTabs(controller);
		SplitPane split = new SplitPane();
		split.setOrientation(Orientation.HORIZONTAL);
		split.getItems().addAll(navRoot, viewRoot);
		split.setDividerPositions(0.333);
		root.setCenter(split);
		viewRoot.setCenter(tabs);
		// Navigation
		updateWorkspaceNavigator();
		Recaf.getWorkspaceSetListeners().add(w -> updateWorkspaceNavigator());
		// Create scene & display the window
		Scene scene = new Scene(root, 800, 600);
		controller.windows().reapplyStyle(scene);
		stage.setScene(scene);
	}


	private void updateWorkspaceNavigator() {
		navRoot.setCenter(new WorkspaceNavigator(controller));
	}

	/**
	 * @param resource
	 * 		Resource containing the class.
	 * @param name
	 * 		Name of class to open.
	 */
	public void openClass(JavaResource resource, String name) {
		// TODO: Tab support (with docking, would be nice)
		tabs.openClass(resource, name);

	}

	/**
	 * @param resource
	 * 		Resource containing the resource.
	 * @param name
	 * 		Name of resource to open.
	 */
	public void openResource(JavaResource resource, String name) {
		tabs.openResource(resource, name);
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
