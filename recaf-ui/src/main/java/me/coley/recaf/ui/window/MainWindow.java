package me.coley.recaf.ui.window;

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
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.threading.ThreadUtil;
import me.coley.recaf.workspace.Workspace;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/**
 * Main window for Recaf.
 *
 * @author Matt Coley
 */
public class MainWindow extends WindowBase {
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
		DockingRegion region1Content = docking.createRegion();
		DockingRegion region2Logging = docking.createRegion();

		// Make the content region not closable.
		// This is the region where we want to display our main content such as classes/files.
		region1Content.setCloseIfEmpty(false);

		// Create tab content
		CompletableFuture<DockTab> workspaceFuture = ThreadUtil.run(() -> new DockTab(Lang.getBinding("workspace.title"), WorkspacePane.getInstance()));
		CompletableFuture<DockTab> loggingFuture = ThreadUtil.run(() -> new DockTab(Lang.getBinding("logging.title"), LoggingTextArea.getInstance()));
		DockTab workspaceTab = workspaceFuture.join();
		DockTab loggingTab = loggingFuture.join();
		workspaceTab.setClosable(false);
		loggingTab.setClosable(false);

		// Populate regions with tabs
		docking.createTabIn(region0Workspace, () -> workspaceTab);
		docking.createTabIn(region1Content, () -> new DockTab(Lang.getBinding("welcome.title"), new WelcomePane()));
		docking.createTabIn(region2Logging, () -> loggingTab);

		// Create splits for docking regions
		SplitPane horizontalSplit = new SplitPane(region0Workspace, region1Content);
		SplitPane verticalSplit = new SplitPane(horizontalSplit, region2Logging);
		horizontalSplit.setDividerPosition(0, 0.30);
		verticalSplit.setDividerPosition(0, 0.76);
		verticalSplit.setOrientation(Orientation.VERTICAL);

		// Prevent these regions from auto-scaling when parent window size changes.
		SplitPane.setResizableWithParent(region0Workspace, false);
		SplitPane.setResizableWithParent(region2Logging, false);

		// Remove workspace/logging from history so new files open in the larger 'region 1'
		docking.removeInteractionHistory(region0Workspace);
		docking.removeInteractionHistory(region2Logging);

		// Wrap it up, add menu to top
		BorderPane root = new BorderPane(verticalSplit);
		root.setTop(MainMenu.getInstance());
		return new Scene(root);
	}

	/**
	 * Configures the doc icon to use the Recaf logo on MacOS platforms.
	 */
	private static void setupMacDockIcon() {
		try {
			// "com.apple.eawt.Application is platform-specific class
			InputStream iconStream = MainWindow.class.getResourceAsStream("/" + Icons.LOGO);
			if (iconStream != null) {
				BufferedImage image = ImageIO.read(iconStream);
				Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
				Object application = applicationClass.getMethod("getApplication").invoke(null);
				Method dockIconSetter = applicationClass.getMethod("setDockIconImage", java.awt.Image.class);
				dockIconSetter.invoke(application, image);
			}
		} catch (Exception ignored) {
			// Expected for non mac platforms
		}
	}

	static {
		setupMacDockIcon();
	}
}
