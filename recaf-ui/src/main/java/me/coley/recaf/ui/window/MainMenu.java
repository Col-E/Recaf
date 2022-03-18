package me.coley.recaf.ui.window;

import javafx.scene.Node;
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
import me.coley.recaf.ui.control.NavigationBar;
import me.coley.recaf.ui.control.menu.ClosableActionMenuItem;
import me.coley.recaf.ui.docking.DockTab;
import me.coley.recaf.ui.docking.RecafDockingManager;
import me.coley.recaf.ui.pane.InfoPane;
import me.coley.recaf.ui.pane.ScriptEditorPane;
import me.coley.recaf.ui.pane.SearchPane;
import me.coley.recaf.ui.prompt.WorkspaceActionType;
import me.coley.recaf.ui.prompt.WorkspaceIOPrompts;
import me.coley.recaf.ui.util.Help;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;

import java.awt.*;
import java.io.File;
import java.nio.file.Path;
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
	private final MenuLabel status = new MenuLabel("Status: IDLE");
	private final Menu menuRecent = menu("menu.file.recent", Icons.RECENT);
	private final Menu menuSearch = menu("menu.search", Icons.ACTION_SEARCH);
	private final Menu menuMappings = menu("menu.mappings", Icons.DOCUMENTATION);
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

		Menu menuScripting = menu("menu.scripting", Icons.CODE);
		menuScripting.getItems().add(action("menu.scripting.new", Icons.PLUS, this::newScript));
		menuScripting.getItems().add(action("menu.scripting.open", Icons.OPEN_FILE, this::openScript));

		menu.getMenus().add(menuFile);
		menu.getMenus().add(menuConfig);
		menu.getMenus().add(menuSearch);
		menu.getMenus().add(menuMappings);
		menu.getMenus().add(menuScripting);
		menu.getMenus().add(menuHelp);
		setCenter(menu);

		setBottom(NavigationBar.getInstance());

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

			Node graphic = Icons.getIconView(Icons.FILE_JAR);
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
		WorkspaceIOPrompts.handleFiles(files, WorkspaceActionType.ADD_TO_WORKSPACE);
	}

	private void openWorkspace() {
		List<Path> files = WorkspaceIOPrompts.promptWorkspaceFiles();
		WorkspaceIOPrompts.handleFiles(files, WorkspaceActionType.CREATE_NEW_WORKSPACE);
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

	private void newScript() {
		ScriptEditorPane scriptEditor = new ScriptEditorPane();
		RecafDockingManager docking = RecafDockingManager.getInstance();
		DockTab scriptEditorTab = docking.createTab(() -> new DockTab(Lang.get("menu.scripting.editor"), scriptEditor));
		scriptEditorTab.setGraphic(Icons.getIconView(Icons.CODE));
		scriptEditor.setTab(scriptEditorTab);
	}

	private void openScript() {
		ScriptEditorPane scriptEditor = new ScriptEditorPane();
		File file = scriptEditor.openFile();
		if (file != null) {
			RecafDockingManager docking = RecafDockingManager.getInstance();
			DockTab scriptEditorTab = docking.createTab(() -> new DockTab(Lang.get("menu.scripting.editor"), scriptEditor));
			scriptEditorTab.setGraphic(Icons.getIconView(Icons.CODE));
			scriptEditor.setTab(scriptEditorTab);
			scriptEditor.setTitle();
		}
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
		itemAddToWorkspace.setDisable(isEmpty);
		itemExportPrimary.setDisable(isEmpty);
		itemClose.setDisable(isEmpty);
		menuSearch.setDisable(isEmpty);
		menuMappings.setDisable(isEmpty);
		menuRecent.setDisable(menuRecent.getItems().isEmpty());
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
