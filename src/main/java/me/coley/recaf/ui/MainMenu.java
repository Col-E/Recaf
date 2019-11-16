package me.coley.recaf.ui;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import me.coley.recaf.command.impl.Export;
import me.coley.recaf.config.ConfBackend;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.search.QueryType;
import me.coley.recaf.ui.controls.*;
import me.coley.recaf.ui.controls.search.SearchPane;
import me.coley.recaf.workspace.WorkspaceIO;
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
		this.controller = controller;
		//
		mFile = new Menu(translate("ui.menubar.file"));
		mFileRecent = new Menu(translate("ui.menubar.file.recent"));
		updateRecent();
		mFile.getItems().addAll(
				new ActionMenuItem(translate("ui.menubar.file.load"), this::load),
				new ActionMenuItem(translate("ui.menubar.file.saveapp"), this::saveApplication),
				new ActionMenuItem(translate("ui.menubar.file.saveworkspace"), this::saveWorkspace),
				mFileRecent);
		mConfig = new ActionMenu(translate("ui.menubar.config"), this::showConfig);
		mSearch = new Menu(translate("ui.menubar.search"));
		mSearch.getItems().addAll(
				new ActionMenuItem(translate("ui.menubar.search.string"), this::searchString),
				new ActionMenuItem(translate("ui.menubar.search.value"), this::searchValue),
				new ActionMenuItem(translate("ui.menubar.search.reference"), () -> {}),
				new ActionMenuItem(translate("ui.menubar.search.declare"), () -> {}),
				new ActionMenuItem(translate("ui.menubar.search.insn"), () -> {}));
		// TODO: These
		mHistory = new Menu(translate("ui.menubar.history"));
		mAttach = new Menu(translate("ui.menubar.attach"));
		mPlugins = new Menu(translate("ui.menubar.plugins"));
		mHelp = new Menu(translate("ui.menubar.help"));
		//
		mHistory.setDisable(true);
		mAttach.setDisable(true);
		mPlugins.setDisable(true);
		mHelp.setDisable(true);
		//
		getMenus().addAll(mFile, mConfig, mSearch, mHistory, mAttach, mPlugins, mHelp);
		// Setup file-choosers
		ExtensionFilter filter = new ExtensionFilter(translate("ui.fileprompt.open.extensions"),
				"*.jar", "*.class", "*.json");
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

	private void searchString() {
		Stage stage  = controller.windows().window(
				translate("ui.menubar.search") + ":" + translate("ui.menubar.search.string"),
				new SearchPane(controller, QueryType.STRING),
				600, 400);
		stage.show();
		stage.toFront();
	}

	private void searchValue() {
		Stage stage  = controller.windows().window(
				translate("ui.menubar.search") + ":" + translate("ui.menubar.search.value"),
				new SearchPane(controller, QueryType.VALUE),
				600, 400);
		stage.show();
		stage.toFront();
	}

	private void load() {
		fcLoad.setInitialDirectory(config().getRecentLoadDir());
		File file = fcLoad.showOpenDialog(null);
		if(file != null) {
			if(controller.loadWorkspace(file))
				config().onLoad(file);
			updateRecent();
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
			exporter.setWorkspace(controller.getWorkspace());
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

	private void updateRecent() {
		mFileRecent.getItems().clear();
		config().getRecentFiles().forEach(this::addRecentItem);
	}

	private void addRecentItem(String path) {
		File file = new File(path);
		if(file.isFile()) {
			String name = file.getName();
			Node graphic = new IconView(getFileIcon(name));
			mFileRecent.getItems().add(new ActionMenuItem(name, graphic, () -> controller.loadWorkspace(file)));
		} else {
			// Not a valid file, so we remove it from the files list
			config().recentFiles.remove(path);
		}
	}

	private void showConfig() {
		Stage stage = controller.windows().getConfigWindow();
		if(stage == null) {
			stage = controller.windows().window(translate("ui.menubar.config"), new ConfigTabs(controller));
			controller.windows().setConfigWindow(stage);
		}
		stage.show();
		stage.toFront();
	}

	private ConfBackend config() {
		return controller.config().backend();
	}
}
