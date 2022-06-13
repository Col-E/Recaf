package me.coley.recaf.ui;

import me.coley.recaf.RecafUI;
import me.coley.recaf.ui.pane.ConfigPane;
import me.coley.recaf.ui.pane.DiffViewPane;
import me.coley.recaf.ui.pane.PluginManagerPane;
import me.coley.recaf.ui.pane.ScriptManagerPane;
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
	private GenericWindow scriptManagerWindow;
	private GenericWindow pluginManagerWindow;
	private GenericWindow modificationsWindow;

	/**
	 * Initialize the windows.
	 */
	public void initialize() {
		mainWindow = new MainWindow();
		configWindow = new GenericWindow(new ConfigPane());
		scriptManagerWindow = new GenericWindow(ScriptManagerPane.getInstance());
		pluginManagerWindow = new GenericWindow(PluginManagerPane.getInstance());
		modificationsWindow = new GenericWindow(new DiffViewPane(RecafUI.getController()));
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

	/**
	 * @return Scripts window.
	 */
	public GenericWindow getScriptsWindow() {
		return scriptManagerWindow;
	}

	/**
	 * @return Scripts window.
	 */
	public GenericWindow getPluginsWindow() {
		return pluginManagerWindow;
	}

	/**
	 * @return Modifications window.
	 */
	public GenericWindow getModificationsWindow() {
		return modificationsWindow;
	}
}
