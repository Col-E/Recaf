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
import me.coley.recaf.plugin.PluginsManager;
import me.coley.recaf.plugin.api.InternalPlugin;
import me.coley.recaf.plugin.api.WorkspacePlugin;
import me.coley.recaf.ui.controls.ViewportTabs;
import me.coley.recaf.ui.controls.WorkspaceNavigator;
import me.coley.recaf.ui.controls.popup.UpdateWindow;
import me.coley.recaf.ui.controls.view.ClassViewport;
import me.coley.recaf.ui.controls.view.FileViewport;
import me.coley.recaf.util.OSUtil;
import me.coley.recaf.util.ThreadUtil;
import me.coley.recaf.util.UiUtil;
import me.coley.recaf.util.VMUtil;
import me.coley.recaf.util.self.SelfUpdater;
import me.coley.recaf.workspace.InstrumentationResource;
import me.coley.recaf.workspace.JavaResource;
import me.coley.recaf.workspace.Workspace;
import org.plugface.core.annotations.Plugin;

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
	private WorkspaceNavigator navigator;

	private MainWindow(GuiController controller) {
		this.controller = controller;
	}

	@Override
	public void start(Stage stage) throws Exception {
		Platform.setImplicitExit(false);
		// Set instances
		window = this;
		this.stage = stage;
		stage.setOnCloseRequest(e -> controller.exit());
		setup();
		stage.show();
	}

	private void setup() {
		if (OSUtil.getOSType() == OSUtil.MAC) {
			UiUtil.setupMacDockIcon();
		} else {
			stage.getIcons().add(new Image(resource("icons/logo.png")));
		}
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
		PluginsManager.getInstance().addPlugin(new WindowPlugin());
		// Create scene & display the window
		Scene scene = new Scene(root, 800, 600);
		controller.windows().reapplyStyle(scene);
		controller.config().keys().registerMainWindowKeys(controller, stage, scene);
		stage.setScene(scene);
		// Show update prompt
		if (SelfUpdater.hasUpdate())
			UpdateWindow.create(this).show(root);
	}

	private void updateWorkspaceNavigator() {
		Platform.runLater(() -> navRoot.setCenter(navigator = new WorkspaceNavigator(controller)));
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
	 * @return Open class/file tabs.
	 */
	public ViewportTabs getTabs() {
		return tabs;
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
	 * @return Root control.
	 */
	public BorderPane getRoot() {
		return root;
	}

	/**
	 * @return Menubar.
	 */
	public MainMenu getMenubar() {
		return menubar;
	}

	/**
	 * @return Workspace navigator. Will be {@code null} if the current workspace is {@code null}.
	 */
	public WorkspaceNavigator getNavigator() {
		return navigator;
	}

	/**
	 * @param controller
	 * 		Window context.
	 *
	 * @return main window instance.
	 */
	public static MainWindow get(GuiController controller) {
		if (window == null) {
			MainWindow app = window = new MainWindow(controller);
			VMUtil.tkIint();
			Platform.runLater(() -> {
				Stage stage = new Stage();
				try {
					// When Recaf is run as an agent on Java 11+ then there is some additional weird classloader logic
					// with modules where it cannot resolve paths to resources. We can simply set the classloader here
					// to point it to whatever classloader has loaded Recaf.
					if (VMUtil.getVmVersion() >= 11 && InstrumentationResource.isActive() &&
							Thread.currentThread().getContextClassLoader() == null) {
						Thread.currentThread().setContextClassLoader(Recaf.class.getClassLoader());
					}
					// Initialize the JFX app once things are configured.
					app.init();
					app.start(stage);
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
				// Disable CSS logger, it complains a lot about non-issues
				try {
					ManagementFactory.getPlatformMXBean(PlatformLoggingMXBean.class)
							.setLoggerLevel("javafx.css", "OFF");
				} catch (IllegalArgumentException ignored) {
					// Expected: logger may not exist
				}
			});
		}
		return window;
	}

	/**
	 * Clear open tabs and remove the current workspace navigator.
	 */
	public void clear() {
		if (tabs != null)
			tabs.getTabs().clear();
		if (navRoot != null && navRoot.getCenter() != null) {
			WorkspaceNavigator nav = ((WorkspaceNavigator) navRoot.getCenter());
			nav.enablePlaceholder();
			nav.clear("...");
		}
	}

	/**
	 * Clear tab viewports
	 */
	public void clearTabViewports() {
		if (tabs != null)
			ThreadUtil.checkJfxAndEnqueue(() -> tabs.clearViewports());
	}

	/**
	 * Set disability status of window components.
	 *
	 * @param status
	 * 		Disability status.
	 */
	public void disable(boolean status) {
		menubar.setDisable(status);
		if (tabs != null)
			tabs.setDisable(status);
		if (navRoot != null)
			navRoot.setDisable(status);
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

	/**
	 * @param title
	 * 		Window title to set.
	 */
	public void setTitle(String title) {
		ThreadUtil.checkJfxAndEnqueue(() -> stage.setTitle(title));
	}

	@Plugin(name = "MainWindow")
	private final class WindowPlugin implements WorkspacePlugin, InternalPlugin {

		@Override
		public void onOpened(Workspace workspace) {
			updateWorkspaceNavigator();
		}

		@Override
		public void onClosed(Workspace workspace) {
		}

		@Override
		public String getVersion() {
			return Recaf.VERSION;
		}

		@Override
		public String getDescription() {
			return "Main window UI.";
		}
	}
}
