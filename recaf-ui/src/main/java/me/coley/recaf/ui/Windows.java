package me.coley.recaf.ui;

import me.coley.recaf.RecafUI;
import me.coley.recaf.ui.pane.*;
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
	private GenericWindow attachWindow;
	private GenericWindow scriptManagerWindow;
	private GenericWindow pluginManagerWindow;
	private GenericWindow modificationsWindow;
	private GenericWindow mappingViewWindow;

	/**
	 * Initialize the windows.
	 */
	public void initialize() {
		mainWindow = new MainWindow();
		configWindow = new GenericWindow(new ConfigPane());
		attachWindow = new GenericWindow(AttachPane.getInstance());
		scriptManagerWindow = new GenericWindow(ScriptManagerPane.getInstance());
		pluginManagerWindow = new GenericWindow(PluginManagerPane.getInstance());
		modificationsWindow = new GenericWindow(new DiffViewPane(RecafUI.getController()));
		mappingViewWindow = new GenericWindow(AggregateMappingPane.get());
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
	 * @return Attach window.
	 */
	public GenericWindow getAttachWindow() {
		return attachWindow;
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

	/**
	 * @return Aggregate mapping viewer window.
	 */
	public GenericWindow getMappingViewWindow() {
		return mappingViewWindow;
	}
}
