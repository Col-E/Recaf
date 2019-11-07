package me.coley.recaf.ui.controls;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.view.*;
import me.coley.recaf.util.UiUtil;
import me.coley.recaf.workspace.JavaResource;

import java.util.HashMap;
import java.util.Map;

/* TODO:
 *  - Dockable tabs (Can move tabs to external window with its own series of tabs)
 *  - Record tabs for
 *    - classes
 *    - resources
 *  - Close all tabs when new workspace is opened
 *  - Tab updating/reloading
 *    - Class rename
 *    - Class access changes
 *    - Resource rename
 *  - Keybind for moving between tabs (Control + Left/Right?)
 *  - Keybind for next view mode
 *  - Keybind for save (Control + S)
 *  - Keybind for load prior (Control + U)
 *     - Prompt user if intended, ask to show prompt for all loads (config option)
 */

/**
 * TabPane to hold a tab for each {@link EditorViewport}
 *
 * @author Matt
 */
public class ResourceTabs extends TabPane {
	private final GuiController controller;
	private final Map<String, Tab> nameToTab = new HashMap<>();

	/**
	 * @param controller
	 * 		Gui controller.
	 */
	public ResourceTabs(GuiController controller) {
		this.controller = controller;
	}

	public void openClass(JavaResource resource, String name) {
		EditorViewport view = new ClassViewport(controller, resource, name);
		String title = name;
		if(title.contains("/"))
			title = title.substring(title.lastIndexOf("/") + 1);
		Tab tab = new Tab(title, view);
		tab.setGraphic(UiUtil.createClassGraphic(resource, name));
		getTabs().add(tab);
	}

	public void openResource(JavaResource resource, String name) {
		EditorViewport view = new ResourceViewport(controller, resource, name);
		String title = name;
		if(title.contains("/"))
			title = title.substring(title.lastIndexOf("/") + 1);
		Tab tab = new Tab(title, view);
		tab.setGraphic(UiUtil.createFileGraphic(name));
		getTabs().add(tab);
	}
}
