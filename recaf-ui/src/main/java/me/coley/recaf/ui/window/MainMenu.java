package me.coley.recaf.ui.window;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.ControllerListener;
import me.coley.recaf.RecafUI;
import me.coley.recaf.ui.control.MenuLabel;
import me.coley.recaf.ui.pane.SearchPane;
import me.coley.recaf.ui.prompt.WorkspaceActionType;
import me.coley.recaf.ui.prompt.WorkspaceIOPrompts;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Menus;
import me.coley.recaf.workspace.Workspace;

import java.nio.file.Path;
import java.util.List;

/**
 * Menu bar applies to the {@link MainWindow}.
 *
 * @author Matt Coley
 */
public class MainMenu extends BorderPane implements ControllerListener {
	private static MainMenu menu;
	private final MenuLabel status = new MenuLabel("Status: IDLE");
	private final Menu menuRecent = Menus.menu("menu.file.recent", Icons.RECENT);
	private final Menu menuSearch = Menus.menu("menu.search", Icons.ACTION_SEARCH);
	private final MenuItem itemAddToWorkspace;
	private final MenuItem itemExportPrimary;
	private final MenuItem itemClose;

	private MainMenu() {
		Menu menuFile = Menus.menu("menu.file", Icons.WORKSPACE);
		Menu menuConfig = Menus.menu("menu.config", Icons.CONFIG);
		Menu menuHelp = Menus.menu("menu.help", Icons.HELP);

		// Main menu
		MenuBar menu = new MenuBar();
		itemAddToWorkspace = Menus.action("menu.file.addtoworkspace", Icons.PLUS, this::addToWorkspace);
		itemExportPrimary = Menus.action("menu.file.exportapp", Icons.EXPORT, this::exportPrimary);
		itemClose = Menus.action("menu.file.close", Icons.ACTION_DELETE, this::closeWorkspace);
		MenuItem itemQuit = Menus.action("menu.file.quit", Icons.CLOSE, this::quit);
		menuFile.getItems().add(itemAddToWorkspace);
		menuFile.getItems().add(Menus.action("menu.file.openworkspace", Icons.OPEN_FILE, this::openWorkspace));
		menuFile.getItems().add(menuRecent);
		menuFile.getItems().add(Menus.separator());
		menuFile.getItems().add(itemExportPrimary);
		menuFile.getItems().add(Menus.separator());
		menuFile.getItems().add(itemClose);
		menuFile.getItems().add(itemQuit);

		menuSearch.getItems().add(Menus.action("menu.search.string", Icons.QUOTE,
				() -> new GenericWindow(SearchPane.createTextSearch()).show()));
		menuSearch.getItems().add(Menus.action("menu.search.number", Icons.NUMBERS,
				() -> new GenericWindow(SearchPane.createNumberSearch()).show()));
		menuSearch.getItems().add(Menus.action("menu.search.references", Icons.REFERENCE,
				() -> new GenericWindow(SearchPane.createReferenceSearch()).show()));
		menuSearch.getItems().add(Menus.action("menu.search.declarations", Icons.T_STRUCTURE,
				() -> new GenericWindow(SearchPane.createDeclarationSearch()).show()));

		menu.getMenus().add(menuFile);
		menu.getMenus().add(menuConfig);
		menu.getMenus().add(menuSearch);
		menu.getMenus().add(menuHelp);
		setCenter(menu);

		// TODO: Implement these
		menuConfig.setDisable(true);
		menuHelp.setDisable(true);

		// TODO: Fill out recent items
		//  - Instead of making users export workspaces, why not just save workspace data in the recaf directory?
		//  - Recent menu will show workspaces, not individual files

		// Info menu
		//	MenuBar info = new MenuBar();
		//	info.getMenus().add(status);
		//	setRight(info);
		// Initial state
		onNewWorkspace(null, null);
	}

	private void addToWorkspace() {
		List<Path> files = WorkspaceIOPrompts.promptWorkspaceFiles();
		WorkspaceIOPrompts.handleFiles(files, WorkspaceActionType.ADD_TO_WORKSPACE);
	}

	private void openWorkspace() {
		List<Path> files = WorkspaceIOPrompts.promptWorkspaceFiles();
		WorkspaceIOPrompts.handleFiles(files, WorkspaceActionType.CREATE_NEW_WORKSPACE);
	}

	private void exportPrimary() {
		WorkspaceIOPrompts.promptExportApplication();
	}

	private void closeWorkspace() {
		RecafUI.getController().setWorkspace(null);
	}

	private void quit() {
		RecafUI.getWindows().getMainWindow().close();
	}

	@Override
	public void onNewWorkspace(Workspace oldWorkspace, Workspace newWorkspace) {
		boolean isEmpty = newWorkspace == null;
		itemAddToWorkspace.setDisable(isEmpty);
		itemExportPrimary.setDisable(isEmpty);
		itemClose.setDisable(isEmpty);
		menuSearch.setDisable(isEmpty);
		if (!isEmpty) {
			// TODO: Update recent workspaces list
		}
	}

	/**
	 * @return Single instance. Can only be used by {@link MainWindow}.
	 */
	public static MainMenu getInstance() {
		if (menu == null) {
			menu = new MainMenu();
			RecafUI.getController().addListener(menu);
		}
		return menu;
	}
}
