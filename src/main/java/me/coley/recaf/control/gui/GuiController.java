package me.coley.recaf.control.gui;

import javafx.concurrent.Task;
import me.coley.recaf.command.impl.LoadWorkspace;
import me.coley.recaf.control.Controller;
import me.coley.recaf.plugin.PluginKeybinds;
import me.coley.recaf.ui.MainWindow;
import me.coley.recaf.ui.controls.ExceptionAlert;
import me.coley.recaf.util.ThreadUtil;
import me.coley.recaf.workspace.Workspace;

import java.nio.file.Path;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import static me.coley.recaf.util.Log.*;

/**
 * Gui controller.
 *
 * @author Matt
 */
public class GuiController extends Controller {
	private WindowManager windows;

	/**
	 * @param workspace
	 * 		Initial workspace path. Can point to a file to load <i>(class, jar)</i> or a workspace
	 * 		configuration <i>(json)</i>.
	 */
	public GuiController(Path workspace) {
		super(workspace);
	}

	@Override
	public boolean setup() {
		boolean succeed = super.setup();
		windows = new WindowManager(this);
		return succeed;
	}

	@Override
	public void run() {
		windows.setMainWindow(MainWindow.get(this));
		super.run();
		PluginKeybinds.getInstance().setup();
	}


	/**
	 * Asynchronously load a workspace from the given file.
	 *
	 * @param path
	 * 		Path to workspace file.
	 * @param action
	 * 		Additional action to run with success/fail result.
	 */
	public void loadWorkspace(Path path, Consumer<Boolean> action) {
		Task<?> loadTask = loadWorkspace(path);
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
			config().backend().onLoad(path, config().display().getMaxRecent());
			main.getMenubar().updateRecent();
		});
		loadTask.setOnFailed(e -> {
			// Log failure reason
			Throwable t = e.getSource().getException();
			if (t != null) {
				error(t, "Failed to open file: {}", path.getFileName());
				ExceptionAlert.show(t, "Failed to open file: " + path.getFileName());
			}
			// Load failure
			main.status("Failed to open file:\n" + path.getFileName());
			main.disable(false);
			if (action != null)
				action.accept(false);
		});
		ThreadUtil.run(loadTask);
	}

	/**
	 * @param path
	 * 		Path to workspace file to open.
	 *
	 * @return Task to load the given workspace.
	 */
	private Task<?> loadWorkspace(Path path) {
		return new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				LoadWorkspace loader = new LoadWorkspace();
				loader.input = path;
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
				config().backend().onLoad(path, config().display().getMaxRecent());
				// Stop message updates
				future.cancel(true);
				return null;
			}
		};
	}

	/**
	 * @param workspace
	 * 		Workspace to set.
	 */
	@Override
	public void setWorkspace(Workspace workspace) {
		super.setWorkspace(workspace);
		MainWindow mainWindow = windows().getMainWindow();
		// Update title with primary input name
		mainWindow.setTitle("Recaf | " + workspace.getPrimary().getShortName());
	}

	/**
	 * @return Window manager.
	 */
	public WindowManager windows() {
		return windows;
	}
}
