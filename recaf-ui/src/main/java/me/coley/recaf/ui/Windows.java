package me.coley.recaf.ui;

import me.coley.recaf.ui.pane.ConfigPane;
import me.coley.recaf.ui.window.GenericWindow;
import me.coley.recaf.ui.window.MainWindow;

/**
 * Window manager.
 *
 * @author Matt Coley
 */
public class Windows {
	private MainWindow mainWindow;
	private GenericWindow configWindow;

	/**
	 * Initialize the windows.
	 */
	public void initialize() {
		mainWindow = new MainWindow();
		configWindow = new GenericWindow(new ConfigPane());
	}

	/**
	 * @return Main window.
	 */
	public MainWindow getMainWindow() {
		return mainWindow;
	}

	/**
	 * @return Config window.
	 */
	public GenericWindow getConfigWindow() {
		return configWindow;
	}
}
