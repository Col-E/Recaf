package me.coley.recaf.ui;

import com.sun.javafx.application.PlatformImpl;
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
import me.coley.recaf.ui.controls.view.ClassViewport;
import me.coley.recaf.ui.controls.view.FileViewport;
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
	private final GuiController controller;
	private Stage stage;
	private BorderPane root;
	private BorderPane navRoot;
	private BorderPane viewRoot;
	private MainMenu menubar;
	private ViewportTabs tabs;

	private MainWindow(GuiController controller) {
		this.controller = controller;
	}

	@Override
	public void start(Stage stage) throws Exception {
		// Set instances
		window = this;
		this.stage = stage;
		setup();
		stage.show();
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
		SplitPane.setResizableWithParent(navRoot, Boolean.FALSE);
		root.setCenter(split);
		viewRoot.setCenter(tabs);
		// Navigation
		updateWorkspaceNavigator();
		Recaf.getWorkspaceSetListeners().add(w -> updateWorkspaceNavigator());
		// Create scene & display the window
		Scene scene = new Scene(root, 800, 600);
		controller.windows().reapplyStyle(scene);
		controller.config().keys().registerMainWindowKeys(controller, stage, scene);
		stage.setScene(scene);
	}

	private void updateWorkspaceNavigator() {
		Platform.runLater(() -> navRoot.setCenter(new WorkspaceNavigator(controller)));
	}

	/**
	 * @param resource
	 * 		Resource containing the class.
	 * @param name
	 * 		Name of class to open.
	 *
	 * @return Viewport of the class.
	 */
	public ClassViewport openClass(JavaResource resource, String name) {
		return tabs.openClass(resource, name);
	}

	/**
	 * @param name
	 * 		Name of class.
	 *
	 * @return Viewport of the class. {@code null} if the class is not currently open.
	 */
	public ClassViewport getClassViewport(String name) {
		return tabs.getClassViewport(name);
	}

	/**
	 * @param resource
	 * 		Resource containing the resource.
	 * @param name
	 * 		Name of file to open.
	 *
	 * @return Viewport of the file.
	 */
	public FileViewport openFile(JavaResource resource, String name) {
		return tabs.openFile(resource, name);
	}

	/**
	 * @param name
	 * 		Name of file.
	 *
	 * @return Viewport of the file. {@code null} if the file is not currently open.
	 */
	public FileViewport getFileViewport(String name) {
		return tabs.getFileViewport(name);
	}

	/**
	 * Save the current application to a file.
	 */
	public void saveApplication() {
		menubar.saveApplication();
	}

	/**
	 * @return Initial stage.
	 */
	public Stage getStage() {
		return stage;
	}

	/**
	 * @return Menubar.
	 */
	public MainMenu getMenubar() {
		return menubar;
	}

	/**
	 * @param controller
	 * 		Window context.
	 *
	 * @return main window instance.
	 */
	public static MainWindow get(GuiController controller) {
		if(window == null) {
			PlatformImpl.startup(() -> {
				MainWindow app = new MainWindow(controller);
				Stage stage = new Stage();
				try {
					app.start(stage);
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			});
			Platform.runLater(() -> {
				// Disable CSS logger, it complains a lot about non-issues
				ManagementFactory.getPlatformMXBean(PlatformLoggingMXBean.class)
						.setLoggerLevel("javafx.css", "OFF");
			});
			Runtime.getRuntime().addShutdownHook(new Thread(Platform::exit));
		}
		return window;
	}

	/**
	 * Clear open tabs and remove the current workspace navigator.
	 */
	public void clear() {
		if (tabs != null)
			tabs.getTabs().clear();
		if (navRoot != null && navRoot.getCenter() != null)
			((WorkspaceNavigator) navRoot.getCenter()).clear("...");
	}

	/**
	 * Set disability status of window components.
	 *
	 * @param status
	 * 		Disability status.
	 */
	public void disable(boolean status) {
		menubar.setDisable(status);
		if(tabs != null)
			tabs.setDisable(status);
		if(navRoot != null)
			navRoot.setDisable(status);
		if (!status)
			stage.setTitle("Recaf");
	}

	/**
	 * Update the navigation pane with an informational message.<br>
	 * Used when loading workspaces for visual feedback.
	 *
	 * @param status
	 * 		Message to update.
	 */
	public void status(String status) {
		if (navRoot != null && navRoot.isDisable() && navRoot.getCenter() != null)
			((WorkspaceNavigator) navRoot.getCenter()).clear(status);
	}
}