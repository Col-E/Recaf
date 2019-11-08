package me.coley.recaf.ui.controls;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.view.*;
import me.coley.recaf.util.UiUtil;
import me.coley.recaf.workspace.JavaResource;

import java.util.HashMap;
import java.util.Map;

/* TODO:
 *  - Dockable tabs (Can move tabs to external window with its own series of tabs)
 *    - Common docking libs are not cross version (8 & 11) compliant
 *    - Either need two versions for each or find a way that works on both versions
 *  - Close all tabs when new workspace is opened
 *  - Fix tab dropdown menu icons not being sized despite using fit-width/height properties
 *    - Wrapping them in a BorderPane should scale them... but instead they vanish.
 *    - Why do the icons not show up? No clue. But its better than varied icon sizes.
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
public class ViewportTabs extends TabPane {
	private final GuiController controller;
	private final Map<String, Tab> nameToTab = new HashMap<>();

	/**
	 * @param controller
	 * 		Gui controller.
	 */
	public ViewportTabs(GuiController controller) {
		this.controller = controller;
		setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
	}

	/**
	 * @param resource
	 * 		Resource containing the class.
	 * @param name
	 * 		Name of class to open.
	 */
	public void openClass(JavaResource resource, String name) {
		if (checkExist(name))
			return;
		EditorViewport view = new ClassViewport(controller, resource, name);
		Tab tab = createTab(name, view);
		tab.setGraphic(UiUtil.createClassGraphic(resource, name));
		select(tab);
	}

	/**
	 * @param resource
	 * 		Resource containing the resource.
	 * @param name
	 * 		Name of resource to open.
	 */
	public void openFile(JavaResource resource, String name) {
		if (checkExist(name))
			return;
		EditorViewport view = new FileViewport(controller, resource, name);
		Tab tab = createTab(name, view);
		BorderPane wrap = new BorderPane(UiUtil.createFileGraphic(name));
		UiUtil.createFileGraphic(name).fitWidthProperty().bind(wrap.widthProperty());
		tab.setGraphic(wrap);
		select(tab);
	}

	private boolean checkExist(String name) {
		// Check if tab by the given name exists
		Tab tab = nameToTab.get(name);
		if (tab == null)
			return false;
		// Select the tab
		select(tab);
		return true;
	}

	private Tab createTab(String name, EditorViewport view) {
		// Normalize name
		String title = name;
		if(title.contains("/"))
			title = title.substring(title.lastIndexOf("/") + 1);
		Tab tab = new Tab(title, view);
		tab.setClosable(true);
		// Name lookup
		tab.setOnClosed(o -> nameToTab.remove(name));
		nameToTab.put(name, tab);
		// Add and return
		getTabs().add(tab);
		return tab;
	}

	private void select(Tab tab) {
		getSelectionModel().select(tab);
		tab.getContent().requestFocus();
	}
}
