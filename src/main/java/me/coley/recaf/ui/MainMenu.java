package me.coley.recaf.ui;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import me.coley.recaf.command.impl.Export;
import me.coley.recaf.config.ConfBackend;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.search.QueryType;
import me.coley.recaf.ui.controls.*;
import me.coley.recaf.util.ClasspathUtil;
import me.coley.recaf.workspace.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import static me.coley.recaf.util.LangUtil.translate;
import static me.coley.recaf.util.Log.*;
import static me.coley.recaf.util.UiUtil.*;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Primary menu.
 *
 * @author Matt
 */
public class MainMenu extends MenuBar {
	private final FileChooser fcLoad = new FileChooser();
	private final FileChooser fcSaveApp = new FileChooser();
	private final FileChooser fcSaveWorkspace = new FileChooser();
	private final GuiController controller;
	private final Menu mFile;
	private final Menu mFileRecent;
	private final Menu mConfig;
	private final Menu mThemeEditor;
	private final Menu mSearch;
	private final Menu mHistory;
	private final Menu mAttach;
	private final Menu mPlugins;
	private final Menu mHelp;

	/**
	 * @param controller
	 * 		Controller context.
	 */
	public MainMenu(GuiController controller) {
		// TODO: Properly managed disabled state of menu items
		this.controller = controller;
		//
		mFile = new Menu(translate("ui.menubar.file"));
		mFileRecent = new Menu(translate("ui.menubar.file.recent"));
		updateRecent();
		if (InstrumentationResource.isActive()) {
			// Agent file menu
			mFile.getItems().addAll(
					new ActionMenuItem(translate("ui.menubar.file.addlib"), this::addLibrary),
					new ActionMenuItem(translate("ui.menubar.file.saveapp"), this::saveApplication),
					new ActionMenuItem(translate("ui.menubar.file.agentexport"), this::saveAgent));
		} else {
			// Normal file menu
			mFile.getItems().addAll(
					new ActionMenuItem(translate("ui.menubar.file.load"), this::load),
					mFileRecent,
					new ActionMenuItem(translate("ui.menubar.file.addlib"), this::addLibrary),
					new ActionMenuItem(translate("ui.menubar.file.saveapp"), this::saveApplication),
					new ActionMenuItem(translate("ui.menubar.file.saveworkspace"), this::saveWorkspace));
		}
		mConfig = new ActionMenu(translate("ui.menubar.config"), this::showConfig);
		mThemeEditor = new ActionMenu(translate("ui.menubar.themeeditor"), this::showThemeEditor);
		mSearch = new Menu(translate("ui.menubar.search"));
		mSearch.getItems().addAll(
				new ActionMenuItem(translate("ui.menubar.search.string"), this::searchString),
				new ActionMenuItem(translate("ui.menubar.search.value"), this::searchValue),
				new ActionMenuItem(translate("ui.menubar.search.reference"), this::searchReference),
				new ActionMenuItem(translate("ui.menubar.search.declare"),  this::searchDeclaration),
				new ActionMenuItem(translate("ui.menubar.search.insn"),  this::searchInsn));
		mAttach = new ActionMenu(translate("ui.menubar.attach"), this::attach);
		// TODO: These menus
		mHistory = new Menu(translate("ui.menubar.history"));
		mPlugins = new Menu(translate("ui.menubar.plugins"));
		mHelp = new Menu(translate("ui.menubar.help"));
		//
		mHistory.setDisable(true);
		mPlugins.setDisable(true);
		mHelp.setDisable(true);
		//
		getMenus().addAll(mFile, mConfig, /* mThemeEditor, */ mSearch, mHistory);
		if (!InstrumentationResource.isActive() && ClasspathUtil.classExists("com.sun.tools.attach.VirtualMachine"))
			getMenus().add(mAttach);
		getMenus().addAll(mPlugins, mHelp);
		// Setup file-choosers
		ExtensionFilter filter = new ExtensionFilter(translate("ui.fileprompt.open.extensions"),
				"*.jar", "*.war", "*.class", "*.json");
		fcLoad.setTitle(translate("ui.filepropt.open"));
		fcLoad.getExtensionFilters().add(filter);
		fcLoad.setSelectedExtensionFilter(filter);
		fcSaveApp.setTitle(translate("ui.filepropt.export"));
		fcSaveApp.getExtensionFilters().add(filter);
		fcSaveApp.setSelectedExtensionFilter(filter);
		filter = new ExtensionFilter(translate("ui.fileprompt.open.extensions"), "*.json");
		fcSaveWorkspace.setTitle(translate("ui.filepropt.export"));
		fcSaveWorkspace.getExtensionFilters().add(filter);
		fcSaveWorkspace.setSelectedExtensionFilter(filter);
	}

	/**
	 * Open string search window.
	 */
	private void searchString() {
		Stage stage  = controller.windows().window(
				translate("ui.menubar.search") + ":" + translate("ui.menubar.search.string"),
				new SearchPane(controller, QueryType.STRING),
				600, 400);
		stage.show();
		stage.toFront();
	}

	/**
	 * Open value search window.
	 */
	private void searchValue() {
		Stage stage  = controller.windows().window(
				translate("ui.menubar.search") + ":" + translate("ui.menubar.search.value"),
				new SearchPane(controller, QueryType.VALUE),
				600, 400);
		stage.show();
		stage.toFront();
	}

	/**
	 * Open reference search window.
	 */
	private void searchReference() {
		Stage stage  = controller.windows().window(
				translate("ui.menubar.search") + ":" + translate("ui.menubar.search.reference"),
				new SearchPane(controller, QueryType.REFERENCE),
				600, 400);
		stage.show();
		stage.toFront();
	}

	/**
	 * Open declaration search window.
	 */
	private void searchDeclaration() {
		Stage stage  = controller.windows().window(
				translate("ui.menubar.search") + ":" + translate("ui.menubar.search.declare"),
				new SearchPane(controller, QueryType.MEMBER_DEFINITION),
				600, 400);
		stage.show();
		stage.toFront();
	}

	/**
	 * Open instruction text search window.
	 */
	private void searchInsn() {
		Stage stage  = controller.windows().window(
				translate("ui.menubar.search") + ":" + translate("ui.menubar.search.insn"),
				new SearchPane(controller, QueryType.INSTRUCTION_TEXT),
				600, 400);
		stage.show();
		stage.toFront();
	}

	/**
	 * Prompt a file open prompt to load an application.
	 */
	private void load() {
		fcLoad.setInitialDirectory(config().getRecentLoadDir());
		File file = fcLoad.showOpenDialog(null);
		if(file != null) {
			controller.loadWorkspace(file, null);
		}
	}

	/**
	 * Adds a selected resource to the current workspace.
	 */
	private void addLibrary() {
		fcLoad.setInitialDirectory(config().getRecentLoadDir());
		File file = fcLoad.showOpenDialog(null);
		if(file != null) {
			try {
				JavaResource resource = FileSystemResource.of(file);
				controller.getWorkspace().getLibraries().add(resource);
				controller.windows().getMainWindow().getNavigator().refresh();
			} catch(Exception ex) {
				error(ex, "Failed to save application to file: {}", file.getName());
				ExceptionAlert.show(ex, "Failed to save application to file: " + file.getName());
			}
		}
	}

	/**
	 * Save the current application to a file.
	 */
	public void saveApplication() {
		fcSaveApp.setInitialDirectory(config().getRecentSaveAppDir());
		File file = fcSaveApp.showSaveDialog(null);
		if (file != null) {
			Export exporter = new Export();
			exporter.setController(controller);
			exporter.output = file;
			try {
				exporter.call();
				config().recentSaveApp = file.getAbsolutePath();
			} catch(Exception ex) {
				error(ex, "Failed to save application to file: {}", file.getName());
				ExceptionAlert.show(ex, "Failed to save application to file: " + file.getName());
			}
		}
	}

	/**
	 * Display attach window.
	 */
	private void attach() {
		Stage stage = controller.windows().getAttachWindow();
		if(stage == null) {
			stage = controller.windows().window(translate("ui.menubar.attach"), new AttachPane(controller), 800, 600);
			controller.windows().setAttachWindow(stage);
		}
		stage.show();
		stage.toFront();
	}

	/**
	 * Save the current application via instrumentation.
	 */
	public void saveAgent() {
		try {
			InstrumentationResource.getInstance().save();
		} catch(Throwable t) {
			error(t, "Failed to save agent changes");
			ExceptionAlert.show(t, "Failed to save agent changes");
		}
	}

	/**
	 * Save the current workspace to a file.
	 */
	private void saveWorkspace() {
		fcSaveWorkspace.setInitialDirectory(config().getRecentSaveWorkspaceDir());
		File file = fcSaveWorkspace.showSaveDialog(null);
		if (file != null) {
			String json = WorkspaceIO.toJson(controller.getWorkspace());
			try {
				FileUtils.write(file, json, UTF_8);
				config().recentSaveWorkspace = file.getAbsolutePath();
			} catch(IOException ex) {
				error(ex, "Failed to save workspace to file: {}", file.getName());
				ExceptionAlert.show(ex, "Failed to save workspace to file: " + file.getName());
			}
		}
	}

	/**
	 * Update the recent files menu.
	 */
	public void updateRecent() {
		mFileRecent.getItems().clear();
		config().getRecentFiles().forEach(this::addRecentItem);
	}

	/**
	 * @param path
	 * 		Path to add to recent files menu.
	 */
	private void addRecentItem(String path) {
		File file = new File(path);
		if(file.isFile()) {
			String name = file.getName();
			Node graphic = new IconView(getFileIcon(name));
			mFileRecent.getItems().add(new ActionMenuItem(name, graphic, () -> controller.loadWorkspace(file, null)));
		} else {
			// Not a valid file, so we remove it from the files list
			config().recentFiles.remove(path);
		}
	}

	/**
	 * Open config window.
	 */
	private void showConfig() {
		Stage stage = controller.windows().getConfigWindow();
		if(stage == null) {
			stage = controller.windows().window(translate("ui.menubar.config"), new ConfigTabs(controller));
			controller.windows().setConfigWindow(stage);
		}
		stage.show();
		stage.toFront();
	}


	/**
	 * Open theme editor window.
	 */
	private void showThemeEditor() {
		Themes.showThemeEditor(controller);
	}

	/**
	 * @return Private config.
	 */
	private ConfBackend config() {
		return controller.config().backend();
	}
}
