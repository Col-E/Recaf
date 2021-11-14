package me.coley.recaf.ui.window;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.ControllerListener;
import me.coley.recaf.RecafUI;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.RecentWorkspacesConfig;
import me.coley.recaf.mapping.MappingUtils;
import me.coley.recaf.mapping.Mappings;
import me.coley.recaf.mapping.MappingsManager;
import me.coley.recaf.mapping.MappingsTool;
import me.coley.recaf.ui.control.MenuLabel;
import me.coley.recaf.ui.pane.ConfigPane;
import me.coley.recaf.ui.pane.InfoPane;
import me.coley.recaf.ui.pane.SearchPane;
import me.coley.recaf.ui.prompt.WorkspaceActionType;
import me.coley.recaf.ui.prompt.WorkspaceIOPrompts;
import me.coley.recaf.ui.util.Help;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.util.Menus;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;

import java.awt.*;
import java.nio.file.Path;
import java.util.List;

/**
 * Menu bar applies to the {@link MainWindow}.
 *
 * @author Matt Coley
 */
public class MainMenu extends BorderPane implements ControllerListener {
	private static final Logger logger = Logging.get(MainMenu.class);
	private static MainMenu menu;
	private final MenuLabel status = new MenuLabel("Status: IDLE");
	private final Menu menuRecent = Menus.menu("menu.file.recent", Icons.RECENT);
	private final Menu menuSearch = Menus.menu("menu.search", Icons.ACTION_SEARCH);
	private final Menu menuMappings = Menus.menu("menu.mappings", Icons.DOCUMENTATION);
	private final MenuItem itemAddToWorkspace;
	private final MenuItem itemExportPrimary;
	private final MenuItem itemClose;

	private MainMenu() {
		Menu menuFile = Menus.menu("menu.file", Icons.WORKSPACE);
		Menu menuConfig = Menus.actionMenu("menu.config", Icons.CONFIG, this::openConfig);
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
				() -> showSearch("menu.search.string", SearchPane.createTextSearch())));
		menuSearch.getItems().add(Menus.action("menu.search.number", Icons.NUMBERS,
				() -> showSearch("menu.search.number", SearchPane.createNumberSearch())));
		menuSearch.getItems().add(Menus.action("menu.search.references", Icons.REFERENCE,
				() -> showSearch("menu.search.references", SearchPane.createReferenceSearch())));
		menuSearch.getItems().add(Menus.action("menu.search.declarations", Icons.T_STRUCTURE,
				() -> showSearch("menu.search.declarations", SearchPane.createDeclarationSearch())));

		menuHelp.getItems().add(Menus.action("menu.help.sysinfo", Icons.INFO, this::openInfo));
		menuHelp.getItems().add(Menus.action("menu.help.docs", Icons.HELP, Help::openDocumentation));
		menuHelp.getItems().add(Menus.action("menu.help.github", Icons.GITHUB, Help::openGithub));
		menuHelp.getItems().add(Menus.action("menu.help.issues", Icons.GITHUB, Help::openGithubIssues));
		menuHelp.getItems().add(Menus.action("menu.help.discord", Icons.DISCORD, Help::openDiscord));

		MappingsManager mappingsManager = RecafUI.getController().getServices().getMappingsManager();
		Menu menuApply = Menus.menu("menu.mappings.apply");
		Menu menuExport = Menus.menu("menu.mappings.export");
		menuMappings.getItems().addAll(menuApply, menuExport);
		for (MappingsTool mappingsTool : mappingsManager.getRegisteredImpls()) {
			menuApply.getItems().add(Menus.actionLiteral(mappingsTool.getName(), null, () -> openMappings(mappingsTool)));
			//menuExport.getItems().add(Menus.actionLiteral(mappingsTool.getName(), null, () -> exportMappings(mappingsTool)));
		}

		menu.getMenus().add(menuFile);
		menu.getMenus().add(menuConfig);
		menu.getMenus().add(menuSearch);
		menu.getMenus().add(menuMappings);
		menu.getMenus().add(menuHelp);
		setCenter(menu);

		refreshRecent();

		// Info menu
		//	MenuBar info = new MenuBar();
		//	info.getMenus().add(status);
		//	setRight(info);

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
			String iconPath = Icons.FILE_JAR;
			menuRecent.getItems().add(Menus.actionLiteral(title, iconPath, () -> {
				try {
					Workspace workspace = model.loadWorkspace();
					RecafUI.getController().setWorkspace(workspace);
				} catch (Exception ex) {
					Toolkit.getDefaultToolkit().beep();
					Configs.recentWorkspaces().recentWorkspaces.remove(model);
					logger.error("Failed to open recent workspace for '{}'", title, ex);
				}
			}));
		}
	}

	private void addToWorkspace() {
		List<Path> files = WorkspaceIOPrompts.promptWorkspaceFiles();
		WorkspaceIOPrompts.handleFiles(files, WorkspaceActionType.ADD_TO_WORKSPACE);
	}

	private void openWorkspace() {
		List<Path> files = WorkspaceIOPrompts.promptWorkspaceFiles();
		WorkspaceIOPrompts.handleFiles(files, WorkspaceActionType.CREATE_NEW_WORKSPACE);
	}

	private void openMappings(MappingsTool mappingsTool) {
		String mappingsText = WorkspaceIOPrompts.getMappingsFromFile();

		if (mappingsText == null) {
			return;
		}

		Mappings mappings = mappingsTool.parse(mappingsText);
		Resource resource = RecafUI.getController().getWorkspace().getResources().getPrimary();
		// TODO: Check if these flags are correct
		MappingUtils.applyMappings(ClassReader.EXPAND_FRAMES, 0, RecafUI.getController(), resource, mappings);
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
		GenericWindow window = new GenericWindow(new ConfigPane());
		window.setTitle(Lang.get("menu.config"));
		window.getStage().setWidth(1080);
		window.getStage().setHeight(600);
		window.show();
	}

	private void showSearch(String key, SearchPane content) {
		GenericWindow window = new GenericWindow(content);
		window.setTitle(Lang.get(key));
		window.show();
	}

	private void openInfo() {
		GenericWindow window = new GenericWindow(new InfoPane());
		window.setTitle(Lang.get("menu.help.sysinfo"));
		window.show();
	}

	@Override
	public void onNewWorkspace(Workspace oldWorkspace, Workspace newWorkspace) {
		boolean isEmpty = newWorkspace == null;
		itemAddToWorkspace.setDisable(isEmpty);
		itemExportPrimary.setDisable(isEmpty);
		itemClose.setDisable(isEmpty);
		menuSearch.setDisable(isEmpty);
		menuMappings.setDisable(isEmpty);
		menuRecent.setDisable(menuRecent.getItems().isEmpty());
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
