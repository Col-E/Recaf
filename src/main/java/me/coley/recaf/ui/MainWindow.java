package me.coley.recaf.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import me.coley.recaf.Recaf;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.plugin.PluginsManager;
import me.coley.recaf.plugin.api.InternalPlugin;
import me.coley.recaf.plugin.api.WorkspacePlugin;
import me.coley.recaf.ui.controls.MarkdownView;
import me.coley.recaf.ui.controls.ViewportTabs;
import me.coley.recaf.ui.controls.WorkspaceNavigator;
import me.coley.recaf.ui.controls.view.ClassViewport;
import me.coley.recaf.ui.controls.view.FileViewport;
import me.coley.recaf.util.OSUtil;
import me.coley.recaf.util.ThreadUtil;
import me.coley.recaf.util.UiUtil;
import me.coley.recaf.util.VMUtil;
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
		// Display notice of future versions
		tabs.getTabs().add(new Tab("Recaf 4.0.0", generateFutureVersionNotice()));
	}

	private Node generateFutureVersionNotice() {
		String notice = "# Recaf 4.0.0: New major update\n" +
				"\n" +
				"You are currently using Recaf 2.21, which was last updated on March 29th 2022. Since then the team behind Recaf has been working on a large number of new features and major overhauls to existing ones. This list is not dynamically updated, so the later you read this the more additional content will be added that is not described in the following lists:\n" +
				"\n" +
				"### New Features\n" +
				"\n" +
				"- Completely new backend design which allows much more control for plugin developers\n" +
				"- Lightweight *\"plugins\"* in the form of single-Java source scripts that are editable on the fly\n" +
				"  - Like plugins, they can interact with all of Recaf's features, but they are limited to what you can put into a single `.java` source file.\n" +
				"- Dedicated code documentation capabilities\n" +
				"  - Write comments on classes, fields, and methods in plain text\n" +
				"  - See the comments you write as JavaDoc in decompiled code\n" +
				"  - Persistent without actually modifying the contents of the classes you're documenting\n" +
				"- View the type hierarchy of classes as a tree of child and parent types\n" +
				"- View the graph of which methods call other methods\n" +
				"\n" +
				"### Improvements\n" +
				"\n" +
				"- An improved layout & new dark-themed user interface style via AtlantaFX\n" +
				"  - The class modes of `Decompile` and `Table` are now merged into one with decompilation on the left, and the list of fields and methods in a collapsible tab to the right\n" +
				"  - New docking framework support, allowing the display of multiple tabs of content all at once\n" +
				"- An improved assembler system backed by JASM\n" +
				"  - Support for editing the entire class bytecode all at once instead of per method\n" +
				"  - Support for additional attributes being editable from within the assembler such as attached annotations\n" +
				"  - UI for converting blocks of Java source code into JASM assembler code *(Serving to replace the `EXPR` keyword)*\n" +
				"- More flexible search options\n" +
				"- More support for patching obfuscated JAR/ZIP files designed to crash common RE tools\n" +
				"- More support for patching obfuscated classes designed to crash ASM and other bytecode parsing libraries\n" +
				"- Redesigned internal structure to allow more control for plugin development\n" +
				"\n" +
				"## Download & Getting It\n" +
				"\n" +
				"The new major version of Recaf 4.X will be uploaded to [the GitHub releases page](https://github.com/Col-E/Recaf/releases). \n" +
				"\n" +
				"**Requirements**:\n" +
				"\n" +
				"- Java 22 or higher\n" +
				"- JavaFX 21 or higher\n" +
				"\n" +
				"You can either:\n" +
				"- Use [the launcher](https://github.com/Col-E/Recaf-Launcher) that does everything for you\n" +
				"- [Manually download Recaf + JavaFX](https://github.com/Col-E/Recaf-Launcher/blob/master/MANUAL.md)\n" +
				"\n" +
				"## Regarding support for 2.X\n" +
				"\n" +
				"There will be no support for 2.X going forward from here. For new features, bug fixes, and support you'll need to use Recaf 4.X.";
		ScrollPane noticeWrapper = new ScrollPane(new MarkdownView(notice));
		noticeWrapper.setPadding(new Insets(15));
		noticeWrapper.setFitToWidth(true);
		noticeWrapper.setFitToHeight(true);
		return noticeWrapper;
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
