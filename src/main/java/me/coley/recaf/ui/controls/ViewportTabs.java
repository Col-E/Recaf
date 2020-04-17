package me.coley.recaf.ui.controls;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.decompile.DecompileImpl;
import me.coley.recaf.ui.controls.view.*;
import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.util.LangUtil;
import me.coley.recaf.util.UiUtil;
import me.coley.recaf.workspace.JavaResource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/* TODO:
 *  - Dockable tabs (Can move tabs to external window with its own series of tabs)
 *    - Common docking libs are not cross version (8 & 11) compliant
 *    - Either need two versions for each or find a way that works on both versions
 *  - Fix tab dropdown menu icons not being sized despite using fit-width/height properties
 *    - Wrapping them in a BorderPane should scale them... but instead they vanish.
 *    - Why do the icons not show up? No clue. But its better than varied icon sizes.
 *  - Tab updating/reloading
 *    - Class/resource rename
 *    - Class access changes
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
		// Keybind for closing current tab
		setOnKeyPressed(e -> {
			if(controller.config().keys().closeTab.match(e)) {
				Tab current = getSelectionModel().getSelectedItem();
				if(current != null)
					closeTab(current);
				Tab newCurrent = getSelectionModel().getSelectedItem();
				if(newCurrent != null)
					newCurrent.getContent().requestFocus();
			}
		});
	}

	/**
	 * @param resource
	 * 		Resource containing the class.
	 * @param name
	 * 		Name of class to open.
	 *
	 * @return Viewport of the class.
	 */
	public ClassViewport openClass(JavaResource resource, String name) {
		if(nameToTab.containsKey(name))
			return getClassViewport(name);
		// Create new tab
		ClassViewport view = new ClassViewport(controller, resource, name);
		view.updateView();
		Tab tab = createTab(name, view);
		int access = ClassUtil.getAccess(resource.getClasses().get(name));
		tab.setGraphic(UiUtil.createClassGraphic(access));
		// Setup context menu
		Menu menuDecompile = new Menu(LangUtil.translate("decompile.decompiler.name"));
		for (DecompileImpl impl : DecompileImpl.values())
			menuDecompile.getItems().add(new ActionMenuItem(impl.name(), () -> view.setOverrideDecompiler(impl)));
		Menu menuMode = new Menu(LangUtil.translate("display.classmode.name"));
		for (ClassViewport.ClassMode mode : ClassViewport.ClassMode.values())
			menuMode.getItems().add(new ActionMenuItem(mode.name(), () -> view.setOverrideMode(mode)));
		ContextMenu ctx = new ContextMenu();
		ctx.getItems().add(menuDecompile);
		ctx.getItems().add(menuMode);
		tab.setContextMenu(ctx);
		// Select & return
		select(tab);
		return view;
	}

	/**
	 * @param name
	 * 		Name of class to check.
	 *
	 * @return Viewport of the class.
	 */
	public ClassViewport getClassViewport(String name) {
		if(nameToTab.containsKey(name)) {
			// Select the tab
			Tab tab = nameToTab.get(name);
			select(tab);
			return (ClassViewport) tab.getContent();
		}
		return null;
	}

	/**
	 * @param resource
	 * 		Resource containing the resource.
	 * @param name
	 * 		Name of resource to open.
	 *
	 * @return Viewport of the file.
	 */
	public FileViewport openFile(JavaResource resource, String name) {
		if(nameToTab.containsKey(name)) {
			return getFileViewport(name);
		}
		FileViewport view = new FileViewport(controller, resource, name);
		view.updateView();
		Tab tab = createTab(name, view);
		BorderPane wrap = new BorderPane(UiUtil.createFileGraphic(name));
		UiUtil.createFileGraphic(name).fitWidthProperty().bind(wrap.widthProperty());
		tab.setGraphic(wrap);
		select(tab);
		return view;
	}

	/**
	 * @param name
	 * 		Name of file to check.
	 *
	 * @return Viewport of the file.
	 */
	public FileViewport getFileViewport(String name) {
		if(nameToTab.containsKey(name)) {
			// Select the tab
			Tab tab = nameToTab.get(name);
			select(tab);
			return (FileViewport) tab.getContent();
		}
		return null;
	}

	/**
	 * Clears viewports
	 */
	public void clearViewports() {
		new HashSet<>(nameToTab.values()).forEach(this::closeTab);
	}

	/**
	 * Closes a tab by the given name.
	 *
	 * @param name
	 * 		Name of item open in the target tab.
	 */
	public void closeTab(String name) {
		Tab tab = nameToTab.get(name);
		if(tab != null)
			closeTab(tab);
	}

	private void closeTab(Tab tab) {
		// Call close handler
		EventHandler<Event> handler = tab.getOnClosed();
		if(handler != null)
			handler.handle(null);
		// Actually close tab
		getTabs().remove(tab);
	}

	/**
	 * @param key
	 * 		Tab name.
	 *
	 * @return {@code true} if a tab by the name is open, otherwise {@code false}.
	 */
	public boolean isOpen(String key) {
		return nameToTab.containsKey(key);
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

	/**
	 * @param name
	 * 		Tab name.
	 *
	 * @return Instance matching name.
	 */
	public Tab getTab(String name) {
		return nameToTab.get(name);
	}

	/**
	 * Select the given tab.
	 *
	 * @param tab
	 * 		Tab to select.
	 */
	public void select(Tab tab) {
		getSelectionModel().select(tab);
		tab.getContent().requestFocus();
	}
}
