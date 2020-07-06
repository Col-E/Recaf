package me.coley.recaf.ui;

import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import me.coley.recaf.Recaf;
import me.coley.recaf.command.impl.Export;
import me.coley.recaf.config.ConfBackend;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.mapping.MappingImpl;
import me.coley.recaf.plugin.PluginsManager;
import me.coley.recaf.plugin.api.MenuProviderPlugin;
import me.coley.recaf.search.QueryType;
import me.coley.recaf.ui.controls.*;
import me.coley.recaf.util.ClasspathUtil;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.self.SelfUpdater;
import me.coley.recaf.workspace.*;
import org.apache.commons.io.FileUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static me.coley.recaf.util.LangUtil.translate;
import static me.coley.recaf.util.Log.error;
import static me.coley.recaf.util.UiUtil.getFileIcon;

/**
 * Primary menu.
 *
 * @author Matt
 */
public class MainMenu extends MenuBar {
	private final FileChooser fcLoadApp = new FileChooser();
	private final FileChooser fcLoadMap = new FileChooser();
	private final FileChooser fcSaveApp = new FileChooser();
	private final FileChooser fcSaveWorkspace = new FileChooser();
	private final GuiController controller;
	private final Menu mFile;
	private final Menu mFileRecent;
	private final Menu mMapping;
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
		mMapping = new Menu(translate("ui.menubar.mapping"));
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
			// Mapping menu
			Menu mApply = new Menu(translate("ui.menubar.mapping.apply"));
			for (MappingImpl impl : MappingImpl.values())
				mApply.getItems().add(new ActionMenuItem(impl.getDisplay(), () -> applyMap(impl)));
			mMapping.getItems().add(mApply);
		}
		mConfig = new ActionMenu(translate("ui.menubar.config"), this::showConfig);
		mThemeEditor = new ActionMenu(translate("ui.menubar.themeeditor"), this::showThemeEditor);
		mSearch = new Menu(translate("ui.menubar.search"));
		mSearch.getItems().addAll(
				new ActionMenuItem(translate("ui.menubar.search.string"), this::searchString),
				new ActionMenuItem(translate("ui.menubar.search.value"), this::searchValue),
				new ActionMenuItem(translate("ui.menubar.search.cls_reference"), this::searchClassReference),
				new ActionMenuItem(translate("ui.menubar.search.mem_reference"), this::searchMemberReference),
				new ActionMenuItem(translate("ui.menubar.search.declare"),  this::searchDeclaration),
				new ActionMenuItem(translate("ui.menubar.search.insn"),  this::searchInsn));
		mAttach = new ActionMenu(translate("ui.menubar.attach"), this::attach);
		mHistory = new ActionMenu(translate("ui.menubar.history"), this::showHistory);
		mHelp = new Menu(translate("ui.menubar.help"));
		if (SelfUpdater.hasUpdate()) {
			mHelp.getItems().add(0,
					new ActionMenuItem(translate("ui.menubar.help.update") + SelfUpdater.getLatestVersion(),
							this::showUpdatePrompt));
		}
		mHelp.getItems().addAll(
				new ActionMenuItem(translate("ui.menubar.help.documentation"), this::showDocumentation),
				new ActionMenuItem(translate("ui.menubar.help.info"), this::showInformation),
				new ActionMenuItem(translate("ui.menubar.help.contact"), this::showContact)
		);
		mPlugins = new Menu(translate("ui.menubar.plugins"));
		if (PluginsManager.getInstance().hasPlugins())
			mPlugins.getItems()
					.add(new ActionMenuItem(translate("ui.menubar.plugins.manage"), this::openPluginManager));
		mPlugins.getItems()
				.add(new ActionMenuItem(translate("ui.menubar.plugins.opendir"), this::openPluginDirectory));
		if (!PluginsManager.getInstance().ofType(MenuProviderPlugin.class).isEmpty()) {
			mPlugins.getItems().add(new SeparatorMenuItem());
			PluginsManager.getInstance().ofType(MenuProviderPlugin.class).forEach(plugin -> {
				mPlugins.getItems().add(plugin.createMenu());
			});
		}
		//
		getMenus().addAll(mFile, mConfig, /* mThemeEditor, */ mSearch, mHistory);
		if (!InstrumentationResource.isActive()) {
			if (ClasspathUtil.classExists("com.sun.tools.attach.VirtualMachine")) {
				getMenus().add(mAttach);
			}
			getMenus().add(mMapping);
		}
		getMenus().addAll(mPlugins, mHelp);
		// Setup file-choosers
		ExtensionFilter filter = new ExtensionFilter(translate("ui.fileprompt.open.extensions"),
				"*.jar", "*.war", "*.class", "*.json");
		fcLoadApp.setTitle(translate("ui.fileprompt.open"));
		fcLoadApp.getExtensionFilters().add(filter);
		fcLoadApp.setSelectedExtensionFilter(filter);
		fcSaveApp.setTitle(translate("ui.fileprompt.export"));
		fcSaveApp.getExtensionFilters().add(filter);
		fcSaveApp.setSelectedExtensionFilter(filter);
		filter = new ExtensionFilter(translate("ui.fileprompt.open.extensions"),
				"*.txt", "*.map", "*.mapping", "*.enigma", "*.pro", "*.srg", "*.tiny", "*.tinyv2");
		fcLoadMap.setTitle(translate("ui.fileprompt.open"));
		fcLoadMap.getExtensionFilters().add(filter);
		fcLoadMap.setSelectedExtensionFilter(filter);
		filter = new ExtensionFilter(translate("ui.fileprompt.open.extensions"), "*.json");
		fcSaveWorkspace.setTitle(translate("ui.fileprompt.export"));
		fcSaveWorkspace.getExtensionFilters().add(filter);
		fcSaveWorkspace.setSelectedExtensionFilter(filter);
	}

	/**
	 * Open string search window.
	 *
	 * @return Search window.
	 */
	public SearchPane searchString() {
		return search(QueryType.STRING, "string");
	}

	/**
	 * Open value search window.
	 *
	 * @return Search window.
	 */
	public SearchPane searchValue() {
		return search(QueryType.VALUE, "value");
	}

	/**
	 * Open class reference search window.
	 *
	 * @return Search window.
	 */
	public SearchPane searchClassReference() {
		return search(QueryType.CLASS_REFERENCE, "cls_reference");
	}

	/**
	 * Open member reference search window.
	 *
	 * @return Search window.
	 */
	public SearchPane searchMemberReference() {
		return search(QueryType.MEMBER_REFERENCE, "mem_reference");
	}

	/**
	 * Open declaration search window.
	 *
	 * @return Search window.
	 */
	public SearchPane searchDeclaration() {
		return search(QueryType.MEMBER_DEFINITION, "declare");
	}

	/**
	 * Open instruction text search window.
	 *
	 * @return Search window.
	 */
	public SearchPane searchInsn() {
		return search(QueryType.INSTRUCTION_TEXT, "insn");
	}

	private SearchPane search(QueryType type, String key) {
		SearchPane pane = new SearchPane(controller, type);
		Stage stage  = controller.windows().window(
				translate("ui.menubar.search") + ":" + translate("ui.menubar.search." + key),
				pane, 600, 400);
		stage.show();
		stage.toFront();
		return pane;
	}

	/**
	 * Prompt a file open prompt to load an application.
	 */
	private void load() {
		fcLoadApp.setInitialDirectory(config().getRecentLoadDir());
		File file = fcLoadApp.showOpenDialog(null);
		if(file != null) {
			controller.loadWorkspace(IOUtil.toPath(file), null);
		}
	}

	/**
	 * Adds a selected resource to the current workspace.
	 */
	private void addLibrary() {
		final Workspace workspace = controller.getWorkspace();

		if (workspace == null)
			return;

		fcLoadApp.setInitialDirectory(config().getRecentLoadDir());
		List<File> files = fcLoadApp.showOpenMultipleDialog(null);

		if (files != null) {

			for (File file : files) {
				try {
					JavaResource resource = FileSystemResource.of(file.toPath());

					workspace.getLibraries().add(resource);
					controller.windows().getMainWindow().getNavigator().refresh();
				} catch(Exception ex) {
					error(ex, "Failed to add library: {}", file.getName());
					ExceptionAlert.show(ex, "Failed to add library: " + file.getName());
				}
			}
		}
	}

	/**
	 * Save the current application to a file.
	 */
	public void saveApplication() {
		if (controller.getWorkspace() == null) {
			return;
		}
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
	 * Load a file and apply mappings of the given type.
	 *
	 * @param impl Mapping implementation type.
	 */
	private void applyMap(MappingImpl impl) {
		fcLoadMap.setInitialDirectory(config().getRecentLoadDir());
		File file = fcLoadMap.showOpenDialog(null);
		if (file != null) {
			try {
				impl.create(file.toPath(), controller.getWorkspace())
						.accept(controller.getWorkspace().getPrimary());
			} catch (Exception ex) {
				error(ex, "Failed to apply mappings: {}", file.getName());
				ExceptionAlert.show(ex, "Failed to apply mappings: " + file.getName());
			}
		}
	}

	/**
	 * Show update prompt.
	 */
	public void showUpdatePrompt() {
		Stage stage = controller.windows()
				.window(translate("ui.menubar.help.update") + SelfUpdater.getLatestVersion(),
						new UpdatePane(controller));
		stage.show();
		stage.toFront();
	}

	/**
	 * Display history window.
	 */
	private void showHistory() {
		Stage stage = controller.windows().getHistoryWindow();
		if(stage == null) {
			stage = controller.windows().window(translate("ui.menubar.history"), new HistoryPane(controller), 800, 600);
			controller.windows().setHistoryWindow(stage);
		}
		stage.show();
		stage.toFront();
	}

	/**
	 * Display contact information window.
	 */
	private void showContact() {
		Stage stage = controller.windows().getContactWindow();
		if(stage == null) {
			stage = controller.windows().window(translate("ui.menubar.help.contact"), new ContactInfoPane());
			controller.windows().setContactWindow(stage);
		}
		stage.show();
		stage.toFront();
	}

	/**
	 * Open documentation page in browser.
	 */
	private void showDocumentation() {
		try {
			Desktop.getDesktop().browse(new URL(Recaf.DOC_URL).toURI());
		} catch(Exception ex) {
			Log.error(ex, "Failed to open documentation url");
		}
	}

	/**
	 * Display system information window.
	 */
	private void showInformation() {
		Stage stage = controller.windows().getInformationWindow();
		if(stage == null) {
			stage = controller.windows().window(translate("ui.menubar.help.info"), new SysInfoPane());
			controller.windows().setInformationWindow(stage);
		}
		stage.show();
		stage.toFront();
	}

	/**
	 * Display system information window.
	 */
	private void openPluginManager() {
		Stage stage = controller.windows().getPluginsWindow();
		if(stage == null) {
			stage = controller.windows().window(translate("ui.menubar.plugins"), new PluginManagerPane(), 600, 233);
			controller.windows().setPluginsWindow(stage);
		}
		stage.show();
		stage.toFront();
	}

	/**
	 * Open plugin directory.
	 */
	private void openPluginDirectory() {
		try {
			Desktop.getDesktop().browse(Recaf.getDirectory("plugins").toUri());
		} catch(IOException ex) {
			Log.error(ex, "Failed to open plugins directory");
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
		if (controller.getWorkspace() == null) {
			return;
		}
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
		Path fspath = Paths.get(path);
		if(Files.exists(fspath)) {
			String name = fspath.getFileName().toString();
			Node graphic = new IconView(getFileIcon(name));
			mFileRecent.getItems().add(new ActionMenuItem(name, graphic, () -> controller.loadWorkspace(fspath, null)));
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
