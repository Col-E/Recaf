package me.coley.recaf.ui;

import me.coley.recaf.ui.window.MainWindow;

/**
 * Window manager.
 *
 * @author Matt Coley
 */
public class Windows {
	private MainWindow mainWindow;

	/**
	 * Initialize the windows.
	 */
	public void initialize() {
		mainWindow = new MainWindow();
	}

	/**
	 * @return Main window.
	 */
	public MainWindow getMainWindow() {
		return mainWindow;
	}
}
