package me.coley.recaf.control.gui;

import javafx.concurrent.Task;
import me.coley.recaf.command.impl.LoadWorkspace;
import me.coley.recaf.control.Controller;
import me.coley.recaf.plugin.PluginKeybinds;
import me.coley.recaf.ui.MainWindow;
import me.coley.recaf.ui.controls.ExceptionAlert;
import me.coley.recaf.util.ThreadUtil;

import java.io.File;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import static me.coley.recaf.util.Log.error;

/**
 * Gui controller.
 *
 * @author Matt
 */
public class GuiController extends Controller {
	private final WindowManager windows = new WindowManager(this);

	/**
	 * @param workspace
	 * 		Initial workspace file. Can point to a file to load <i>(class, jar)</i> or a workspace
	 * 		configuration <i>(json)</i>.
	 */
	public GuiController(File workspace) {
		super(workspace);
	}

	@Override
	public void run() {
		super.run();
		PluginKeybinds.getInstance().setup();
		windows.setMainWindow(MainWindow.get(this));
	}

	/**
	 * Asynchronously load a workspace from the given file.
	 *
	 * @param file
	 * 		Workspace file to open.
	 * @param action
	 * 		Additional action to run with success/fail result.
	 */
	public void loadWorkspace(File file, Consumer<Boolean> action) {
		Task<Boolean> loadTask = loadWorkspace(file);
		MainWindow main = windows.getMainWindow();
		loadTask.messageProperty().addListener((n, o, v) -> main.status(v));
		loadTask.setOnRunning(e -> {
			// Clear current items since we want to load a new workspace
			main.clear();
			main.disable(true);
		});
		loadTask.setOnSucceeded(e -> {
			// Load success
			main.disable(false);
			if (action != null)
				action.accept(true);
			// Update recently loaded
			config().backend().onLoad(file);
			main.getMenubar().updateRecent();
			// Updated cached primary jar to support recompile
			getWorkspace().writePrimaryJarToTemp();
		});
		loadTask.setOnFailed(e -> {
			// Load failure
			main.status("Failed to open file:\n" + file.getName());
			main.disable(false);
			if (action != null)
				action.accept(false);
		});
		ThreadUtil.run(loadTask);
	}

	/**
	 * @param file
	 * 		Workspace file to open.
	 *
	 * @return Task to load the given workspace,
	 * which yields {@code true} if the workspace was loaded successfully.
	 */
	private Task<Boolean> loadWorkspace(File file) {
		return new Task<Boolean>() {
			@Override
			protected Boolean call() throws Exception {
				LoadWorkspace loader = new LoadWorkspace();
				loader.input = file;
				try {
					// This ugly garbage handles updating the UI with the progress message
					// and how long that message has been shown for in seconds/millis
					long updateInterval = 16;
					long start = System.currentTimeMillis();
					ScheduledFuture<?> future = ThreadUtil.runRepeated(updateInterval, () -> {
						long time = System.currentTimeMillis() - start;
						updateMessage(loader.getStatus() +
								String.format("\n- Elapsed: %02d.%02d", time / 1000, (time % 1000)));
					});
					// Start the load process
					setWorkspace(loader.call());
					windows.getMainWindow().clearTabViewports();
					config().backend().onLoad(file);
					// Stop message updates
					future.cancel(true);
					return true;
				} catch(Exception ex) {
					error(ex, "Failed to open file: {}", file.getName());
					ExceptionAlert.show(ex, "Failed to open file: " + file.getName());
					return false;
				}
			}
		};
	}

	/**
	 * @return Window manager.
	 */
	public WindowManager windows() {
		return windows;
	}
}
