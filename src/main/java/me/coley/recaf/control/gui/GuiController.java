package me.coley.recaf.control.gui;

import me.coley.recaf.command.impl.LoadWorkspace;
import me.coley.recaf.config.ConfigManager;
import me.coley.recaf.control.Controller;
import me.coley.recaf.ui.MainWindow;
import me.coley.recaf.ui.controls.ExceptionAlert;

import java.io.File;

import static me.coley.recaf.util.Log.error;

/**
 * Gui controller.
 *
 * @author Matt
 */
public class GuiController extends Controller {
	private final WindowManager windows = new WindowManager(this);
	private final ConfigManager configs = new ConfigManager();

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
		// Initialize
		configs.initialize();
		windows.setMainWindow(MainWindow.get(this));
	}

	/**
	 * @param file
	 * 		Workspace file to open.
	 *
	 * @return {@code true} if the workspace was loaded successfully.
	 */
	public boolean loadWorkspace(File file) {
		LoadWorkspace loader = new LoadWorkspace();
		loader.input = file;
		try {
			setWorkspace(loader.call());
			configs.backend().recentFiles.add(file.getAbsolutePath());
			return true;
		} catch(Exception ex) {
			error(ex, "Failed to open file: {}", file.getName());
			ExceptionAlert.show(ex, "Failed to open file: " + file.getName());
			return false;
		}
	}

	/**
	 * @return Window manager.
	 */
	public WindowManager windows() {
		return windows;
	}

	/**
	 * @return Config manager.
	 */
	public ConfigManager config(){
		return configs;
	}
}
