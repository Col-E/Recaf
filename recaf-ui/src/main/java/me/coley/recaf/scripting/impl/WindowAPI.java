package me.coley.recaf.scripting.impl;

import me.coley.recaf.RecafUI;
import me.coley.recaf.ui.Windows;
import me.coley.recaf.ui.window.GenericWindow;
import me.coley.recaf.ui.window.MainWindow;

/**
 * Utility functions for working with windows.
 *
 * @author Wolfie / win32kbase
 */
public class WindowAPI {
	/**
	 * @return Recaf window manager.
	 */
	public static Windows getWindowManager() {
		return RecafUI.getWindows();
	}

	/**
	 * @return The main window.
	 */
	public static MainWindow getMainWindow() {
		return getWindowManager().getMainWindow();
	}

	/**
	 * @return The config window.
	 */
	public static GenericWindow getConfigWindow() {
		return getWindowManager().getConfigWindow();
	}
}
