package me.coley.recaf.ui.window;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.coley.recaf.cdi.RecafContainer;
import me.coley.recaf.instrument.InstrumentationManager;
import me.coley.recaf.mapping.AggregateMappingManager;
import me.coley.recaf.mapping.MappingsManager;
import me.coley.recaf.ui.menu.MainMenu;
import me.coley.recaf.ui.menu.MenuBarPopulation;
import me.coley.recaf.ui.pane.*;
import me.coley.recaf.workspace.WorkspaceManager;

/**
 * Window manager.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class Windows {
	private final MainWindow mainWindow;
	private final GenericWindow configWindow;
	private final GenericWindow attachWindow;
	private final GenericWindow scriptManagerWindow;
	private final GenericWindow pluginManagerWindow;
	private final GenericWindow modificationsWindow;
	private GenericWindow mappingViewWindow;

	@Inject
	public Windows(WorkspaceManager workspaceManager,
				   InstrumentationManager instrumentationManager,
				   MappingsManager mappingsManager,
				   ScriptManagerPane scriptManagerPane,
				   MenuBarPopulation menuBarPopulation,
				   MainMenu menu) {
		menuBarPopulation.setWindows(this);
		mainWindow = new MainWindow(menu, workspaceManager);
		configWindow = new GenericWindow(new ConfigPane());
		attachWindow = new GenericWindow(new AttachPane(instrumentationManager));
		scriptManagerWindow = new GenericWindow(scriptManagerPane);
		pluginManagerWindow = new GenericWindow(PluginManagerPane.getInstance());
		modificationsWindow = new GenericWindow(new DiffViewPane(workspaceManager));
		// Dummy display until a workspace is opened
		mappingViewWindow = new GenericWindow(new AggregateMappingPane(new AggregateMappingManager(), mappingsManager));
		// When a new workspace is opened, we will create a new window for the new aggregate mappings display
		workspaceManager.addWorkspaceOpenListener(w -> {
			AggregateMappingPane aggregateMappingPane = RecafContainer.get(AggregateMappingPane.class);
			mappingViewWindow = new GenericWindow(aggregateMappingPane);
		});
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
