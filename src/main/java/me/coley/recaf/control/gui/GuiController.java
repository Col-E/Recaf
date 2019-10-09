package me.coley.recaf.control.gui;

import me.coley.recaf.config.ConfigManager;
import me.coley.recaf.control.Controller;
import me.coley.recaf.ui.MainWindow;

import java.io.File;

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
		configs.initialize();
		windows.setMainWindow(MainWindow.get(this));
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
