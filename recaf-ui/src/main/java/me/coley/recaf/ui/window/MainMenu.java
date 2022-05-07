package me.coley.recaf.ui.window;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import me.coley.recaf.ControllerListener;
import me.coley.recaf.RecafUI;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.RecentWorkspacesConfig;
import me.coley.recaf.mapping.MappingUtils;
import me.coley.recaf.mapping.Mappings;
import me.coley.recaf.mapping.MappingsManager;
import me.coley.recaf.mapping.MappingsTool;
import me.coley.recaf.ui.control.MenuLabel;
import me.coley.recaf.ui.control.NavigationBar;
import me.coley.recaf.ui.control.menu.ClosableActionMenuItem;
import me.coley.recaf.ui.pane.InfoPane;
import me.coley.recaf.ui.pane.SearchPane;
import me.coley.recaf.ui.prompt.WorkspaceActionType;
import me.coley.recaf.ui.prompt.WorkspaceIOPrompts;
import me.coley.recaf.ui.util.Help;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.ThreadUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;

import java.awt.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static me.coley.recaf.ui.util.Menus.*;

/**
 * Menu bar applies to the {@link MainWindow}.
 *
 * @author Matt Coley
 */
public class MainMenu extends BorderPane implements ControllerListener {
	private static final Logger logger = Logging.get(MainMenu.class);
	private static MainMenu menu;
	private final BooleanProperty noWorkspace = new SimpleBooleanProperty(true);
	private final MenuLabel status = new MenuLabel("Status: IDLE");
	private final Menu menuRecent = menu("menu.file.recent", Icons.RECENT);
	private final Menu menuSearch = menu("menu.search", Icons.ACTION_SEARCH);
	private final Menu menuMappings = menu("menu.mappings", Icons.DOCUMENTATION);
	private final Menu menuScripting =  menu("menu.scripting", Icons.CODE);
	private final MenuItem itemAddToWorkspace;
	private final MenuItem itemExportPrimary;
	private final MenuItem itemClose;

	private MainMenu() {
		Menu menuFile = menu("menu.file", Icons.WORKSPACE);
		Menu menuConfig = actionMenu("menu.config", Icons.CONFIG, this::openConfig);
		Menu menuHelp = menu("menu.help", Icons.HELP);

		// Main menu
		MenuBar menu = new MenuBar();
		itemAddToWorkspace = action("menu.file.addtoworkspace", Icons.PLUS, this::addToWorkspace);
		itemExportPrimary = action("menu.file.exportapp", Icons.EXPORT, this::exportPrimary);
		itemClose = action("menu.file.close", Icons.ACTION_DELETE, this::closeWorkspace);

		itemAddToWorkspace.disableProperty().bind(noWorkspace);
		itemExportPrimary.disableProperty().bind(noWorkspace);
		itemClose.disableProperty().bind(noWorkspace);
		menuSearch.disableProperty().bind(noWorkspace);
		menuMappings.disableProperty().bind(noWorkspace);
		SimpleListProperty<MenuItem> recentItemsProperty = new SimpleListProperty<>(menuRecent.getItems());
		menuRecent.disableProperty().bind(recentItemsProperty.emptyProperty());

		MenuItem itemQuit = action("menu.file.quit", Icons.CLOSE, this::quit);
		menuFile.getItems().add(itemAddToWorkspace);
		menuFile.getItems().add(action("menu.file.openworkspace", Icons.OPEN_FILE, this::openWorkspace));
		menuFile.getItems().add(menuRecent);
		menuFile.getItems().add(separator());
		menuFile.getItems().add(itemExportPrimary);
		menuFile.getItems().add(separator());
		menuFile.getItems().add(itemClose);
		menuFile.getItems().add(itemQuit);

		menuSearch.getItems().add(action("menu.search.string", Icons.QUOTE,
				() -> showSearch("menu.search.string", SearchPane.createTextSearch())));
		menuSearch.getItems().add(action("menu.search.number", Icons.NUMBERS,
				() -> showSearch("menu.search.number", SearchPane.createNumberSearch())));
		menuSearch.getItems().add(action("menu.search.references", Icons.REFERENCE,
				() -> showSearch("menu.search.references", SearchPane.createReferenceSearch())));
		menuSearch.getItems().add(action("menu.search.declarations", Icons.T_STRUCTURE,
				() -> showSearch("menu.search.declarations", SearchPane.createDeclarationSearch())));

		menuHelp.getItems().add(action("menu.help.sysinfo", Icons.INFO, this::openInfo));
		menuHelp.getItems().add(action("menu.help.docs", Icons.HELP, Help::openDocumentation));
		menuHelp.getItems().add(action("menu.help.github", Icons.GITHUB, Help::openGithub));
		menuHelp.getItems().add(action("menu.help.issues", Icons.GITHUB, Help::openGithubIssues));
		menuHelp.getItems().add(action("menu.help.discord", Icons.DISCORD, Help::openDiscord));

		MappingsManager mappingsManager = RecafUI.getController().getServices().getMappingsManager();
		Menu menuApply = menu("menu.mappings.apply");
		Menu menuExport = menu("menu.mappings.export");
		menuMappings.getItems().addAll(menuApply, menuExport);
		for (MappingsTool mappingsTool : mappingsManager.getRegisteredImpls()) {
			String name = mappingsTool.getName();
			menuApply.getItems().add(actionLiteral(name, null, () -> openMappings(mappingsTool)));
			if (mappingsTool.supportsTextExport())
				menuExport.getItems().add(actionLiteral(name, null, () -> exportMappings(mappingsTool)));
		}

		updateScriptMenu(null);

		menu.getMenus().add(menuFile);
		menu.getMenus().add(menuConfig);
		menu.getMenus().add(menuSearch);
		menu.getMenus().add(menuMappings);
		menu.getMenus().add(menuScripting);
		menu.getMenus().add(menuHelp);

		NavigationBar nav = NavigationBar.getInstance();

		// Layout the content. The navigation bar goes 'under' the menu.
		// This way it can slide in and out like a drawer.
		StackPane stack = new StackPane();
		stack.getChildren().addAll(nav, menu);
		StackPane.setAlignment(menu, Pos.TOP_LEFT);
		StackPane.setAlignment(nav, Pos.BOTTOM_LEFT);
		setCenter(stack);


		// Info menu
		//	MenuBar info = new MenuBar();
		//	info.getMenus().add(status);
		//	setRight(info);

		// Populate file menu
		refreshRecent();
		// Initial state
		onNewWorkspace(null, null);
	}

	/**
	 * Update the recent workspaces menu.
	 */
	public void refreshRecent() {
		menuRecent.getItems().clear();
		for (RecentWorkspacesConfig.WorkspaceModel model : Configs.recentWorkspaces().recentWorkspaces) {
			int libraryCount = model.getLibraries().size();
			String title;
			if (libraryCount > 0) {
				title = model.getPrimary().getSimpleName() + " + " + libraryCount;
			} else {
				title = model.getPrimary().getSimpleName();
			}

			Node graphic = Icons.getPathIcon(model.getPrimary().getPath());
			menuRecent.getItems().add(new ClosableActionMenuItem(title, graphic, () -> {
				try {
					Workspace workspace = model.loadWorkspace();
					RecafUI.getController().setWorkspace(workspace);
				} catch (Exception ex) {
					Toolkit.getDefaultToolkit().beep();
					Configs.recentWorkspaces().recentWorkspaces.remove(model);
					logger.error("Failed to open recent workspace for '{}'", title, ex);
				}
			}, () -> Configs.recentWorkspaces().recentWorkspaces.remove(model)));
		}
	}

	private void addToWorkspace() {
		List<Path> files = WorkspaceIOPrompts.promptWorkspaceFiles();
		if (!files.isEmpty())
			ThreadUtil.run(() -> WorkspaceIOPrompts.handleFiles(files, WorkspaceActionType.ADD_TO_WORKSPACE));
	}

	private void openWorkspace() {
		List<Path> files = WorkspaceIOPrompts.promptWorkspaceFiles();
		if (!files.isEmpty())
			ThreadUtil.run(() -> WorkspaceIOPrompts.handleFiles(files, WorkspaceActionType.CREATE_NEW_WORKSPACE));
	}

	private void openMappings(MappingsTool mappingsTool) {
		String mappingsText = WorkspaceIOPrompts.promptMappingInput();
		if (mappingsText == null) {
			return;
		}
		Mappings mappings = mappingsTool.create();
		mappings.parse(mappingsText);
		Resource resource = RecafUI.getController().getWorkspace().getResources().getPrimary();
		MappingUtils.applyMappings(ClassReader.EXPAND_FRAMES, 0, RecafUI.getController(), resource, mappings);
	}

	private void exportMappings(MappingsTool mappingsTool) {
		Mappings currentAggregate = RecafUI.getController().getServices().getMappingsManager().getAggregatedMappings();
		if (!currentAggregate.supportsExportIntermediate()) {
			logger.error("Cannot export aggregated mappings, intermediate export not supported!");
			return;
		}
		Mappings targetMappings = mappingsTool.create();
		targetMappings.importIntermediate(currentAggregate.exportIntermediate());
		WorkspaceIOPrompts.promptMappingExport(targetMappings);
	}

	/**
	 * Updates the 'Scripting' menu with a list of menu items.
	 *
	 * @param scriptItems
	 * 		The script menu items
	 */
	public void updateScriptMenu(Collection<MenuItem> scriptItems) {
		menuScripting.getItems().clear();
		if(scriptItems != null && !scriptItems.isEmpty()) {
			menuScripting.getItems().addAll(scriptItems);
			menuScripting.getItems().add(separator());
		}
		menuScripting.getItems().add(action("menu.scripting.manage", Icons.OPEN_FILE, this::openScripts));
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

	private void openConfig() {
		GenericWindow window = RecafUI.getWindows().getConfigWindow();
		window.titleProperty().bind(Lang.getBinding("menu.config"));
		window.getStage().setWidth(1080);
		window.getStage().setHeight(600);
		window.show();
	}

	private void openScripts() {
		GenericWindow window = RecafUI.getWindows().getScriptsWindow();
		window.titleProperty().bind(Lang.getBinding("menu.scripting.manage"));
		window.getStage().setWidth(750);
		window.getStage().setHeight(450);
		window.show();
		window.requestFocus();
	}

	private void showSearch(String key, SearchPane content) {
		GenericWindow window = new GenericWindow(content);
		window.titleProperty().bind(Lang.getBinding(key));
		window.show();
	}

	private void openInfo() {
		GenericWindow window = new GenericWindow(new InfoPane());
		window.titleProperty().bind(Lang.getBinding("menu.help.sysinfo"));
		window.show();
	}

	@Override
	public void onNewWorkspace(Workspace oldWorkspace, Workspace newWorkspace) {
		boolean isEmpty = newWorkspace == null;
		noWorkspace.set(isEmpty);
		if (!isEmpty) {
			Configs.recentWorkspaces().addWorkspace(newWorkspace);
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
