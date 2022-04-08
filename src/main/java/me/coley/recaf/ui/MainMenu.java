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
import me.coley.recaf.mapping.Mappings;
import me.coley.recaf.mapping.TinyV2Mappings;
import me.coley.recaf.plugin.PluginsManager;
import me.coley.recaf.plugin.api.MenuProviderPlugin;
import me.coley.recaf.search.QueryType;
import me.coley.recaf.ui.controls.*;
import me.coley.recaf.ui.controls.pane.*;
import me.coley.recaf.util.*;
import me.coley.recaf.util.self.SelfUpdater;
import me.coley.recaf.workspace.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

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
	private final FileChooser fcSaveMap = new FileChooser();
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
		// Setup mac system menubar
		boolean isUseSystemMenuBar = controller.config().display().useSystemMenubar;
		boolean isEmptyMenuItemsSupported = !isUseSystemMenuBar || OSUtil.getOSType() != OSUtil.MAC;
		setUseSystemMenuBar(isUseSystemMenuBar);
		// Setup menu entries
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
			mFile.getItems().add(
					new ActionMenuItem(translate("ui.menubar.file.load"), this::load));
			if (controller.config().display().getMaxRecent() > 0)
				mFile.getItems().add(mFileRecent);
			mFile.getItems().addAll(
					new ActionMenuItem(translate("ui.menubar.file.addlib"), this::addLibrary),
					new ActionMenuItem(translate("ui.menubar.file.saveapp"), this::saveApplication),
					new ActionMenuItem(translate("ui.menubar.file.saveworkspace"), this::saveWorkspace));
			// Mapping menu
			Menu mApply = new Menu(translate("ui.menubar.mapping.apply"));
			Menu mExport = new Menu(translate("ui.menubar.mapping.export"));
			populateMappingMenus(mApply, mExport);
			mMapping.getItems().add(mApply);
			mMapping.getItems().add(mExport);
		}
		if (isEmptyMenuItemsSupported) {
			mThemeEditor = new ActionMenu(translate("ui.menubar.themeeditor"), this::showThemeEditor);
			mConfig = new ActionMenu(translate("ui.menubar.config"), this::showConfig);
		} else {
			mConfig = new Menu(translate("ui.menubar.config"));
			mConfig.getItems().add(new ActionMenuItem(translate("misc.open"), this::showConfig));
			mThemeEditor = new Menu(translate("ui.menubar.themeeditor"));
			mThemeEditor.getItems().add(new ActionMenuItem(translate("misc.open"), this::showThemeEditor));
		}
		mSearch = new Menu(translate("ui.menubar.search"));
		mSearch.getItems().addAll(
				new ActionMenuItem(translate("ui.menubar.search.string"), this::searchString),
				new ActionMenuItem(translate("ui.menubar.search.value"), this::searchValue),
				new ActionMenuItem(translate("ui.menubar.search.cls_reference"), this::searchClassReference),
				new ActionMenuItem(translate("ui.menubar.search.mem_reference"), this::searchMemberReference),
				new ActionMenuItem(translate("ui.menubar.search.declare"),  this::searchDeclaration),
				new ActionMenuItem(translate("ui.menubar.search.insn"),  this::searchInsn));
		mAttach = new Menu(translate("ui.menubar.attach"));
		mAttach.getItems().addAll(
				new ActionMenuItem(translate("ui.menubar.attach.existing"), this::attachExisting),
				new ActionMenuItem(translate("ui.menubar.attach.create"), this::attachCreate));
		if (isEmptyMenuItemsSupported) {
			mHistory = new ActionMenu(translate("ui.menubar.history"), this::showHistory);
		} else {
			mHistory = new Menu(translate("ui.menubar.history"));
			mHistory.getItems().add(new ActionMenuItem(translate("misc.open"), this::showHistory));
		}
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
		ExtensionFilter loadFilter = new ExtensionFilter(translate("ui.fileprompt.open.extensions"),
				"*.jar", "*.war", "*.class", "*.json");
		ExtensionFilter mappingFilter = new ExtensionFilter(translate("ui.fileprompt.mapping.extensions"),
				"*.txt", "*.map", "*.mapping", "*.enigma", "*.pro", "*.srg", "*.tsrg", "*.tiny", "*.tinyv2", "*.jobf");
		ExtensionFilter saveFilter = new ExtensionFilter(translate("ui.fileprompt.export.extensions"),
				"*.jar", "*.war", "*.class", "*.zip");
		ExtensionFilter saveWorkspaceFilter = new ExtensionFilter(translate("ui.fileprompt.workspace.extensions"),
				"*.json");
		ExtensionFilter saveMapFilter = new ExtensionFilter(translate("ui.fileprompt.export.mapping"),
				"*.txt", "*.map", "*.mapping");
		fcLoadApp.setTitle(translate("ui.fileprompt.open"));
		fcLoadApp.getExtensionFilters().add(loadFilter);
		fcLoadApp.setSelectedExtensionFilter(loadFilter);
		fcSaveApp.setTitle(translate("ui.fileprompt.export"));
		fcSaveApp.getExtensionFilters().add(saveFilter);
		fcSaveApp.setSelectedExtensionFilter(saveFilter);
		fcLoadMap.setTitle(translate("ui.fileprompt.mapping"));
		fcLoadMap.getExtensionFilters().add(mappingFilter);
		fcLoadMap.setSelectedExtensionFilter(mappingFilter);
		fcSaveWorkspace.setTitle(translate("ui.fileprompt.workspace"));
		fcSaveWorkspace.getExtensionFilters().add(saveWorkspaceFilter);
		fcSaveWorkspace.setSelectedExtensionFilter(saveWorkspaceFilter);
		fcSaveMap.setTitle(translate("ui.fileprompt.export.mapping"));
		fcSaveMap.getExtensionFilters().add(saveMapFilter);
		fcSaveMap.setSelectedExtensionFilter(saveMapFilter);
	}

	/**
	 * Add mapping sub-items in the menus.
	 *
	 * @param applyMenu
	 * 		Menu to hold sub-items to apply mappings of a given type.
	 * @param exportMenu
	 * 		Menu to hold sub-items to save as a given type of mappings.
	 */
	private void populateMappingMenus(Menu applyMenu, Menu exportMenu) {
		for (MappingImpl impl : MappingImpl.values()) {
			if (impl == MappingImpl.TINY2) {
				// Edge case since there are multiple ways we can interpret the mapping directions
				Menu tiny2Menu = new Menu(impl.getDisplay());
				for (TinyV2Mappings.TinyV2SubType subType : TinyV2Mappings.TinyV2SubType.values()) {
					tiny2Menu.getItems()
							.add(new ActionMenuItem(subType.toString(), () -> applyTinyV2Map(subType)));
				}
				applyMenu.getItems().add(tiny2Menu);
			} else {
				applyMenu.getItems().add(new ActionMenuItem(impl.getDisplay(), () -> applyMap(impl)));
			}
			// TODO: Rewrite the mapping implementation design to work with both reading and writing
			if (impl == MappingImpl.SIMPLE)  {
				exportMenu.getItems().addAll(new ActionMenuItem(impl.getDisplay(), () -> exportMap(impl)));
			}
		}
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
			exporter.compress = config().compress;
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
	 * Export the current {@link Workspace#getAggregatedMappings() aggregated mappings} to the given format.
	 *
	 * @param impl
	 * 		Mapping implementation to use.
	 */
	private void exportMap(MappingImpl impl) {
		if (controller.getWorkspace() == null) {
			return;
		}

		fcSaveMap.setInitialDirectory(config().getRecentSaveMapDir());
		File file = fcSaveMap.showSaveDialog(null);
		if (file != null) {
			// TODO: Make the Mappings classes do this conversion for their respective format
			String fullMapping = controller.getWorkspace().getAggregatedMappings().entrySet().stream()
					.map(e -> e.getKey() + " " + e.getValue())
					.collect(Collectors.joining("\n"));
			try {
				FileUtils.write(file, fullMapping, UTF_8);
				config().recentSaveMap = file.getAbsolutePath();
			} catch(IOException ex) {
				error(ex, "Failed to save simple mapping to file: {}", file.getName());
				ExceptionAlert.show(ex, "Failed to save simple mapping to file: " + file.getName());
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
				Mappings mappings = impl.create(file.toPath(), controller.getWorkspace());
				mappings.setCheckFieldHierarchy(true);
				mappings.setCheckMethodHierarchy(true);
				mappings.accept(controller.getWorkspace().getPrimary());
			} catch (Exception ex) {
				error(ex, "Failed to apply mappings: {}", file.getName());
				ExceptionAlert.show(ex, "Failed to apply mappings: " + file.getName());
			}
		}
	}

	private void applyTinyV2Map(TinyV2Mappings.TinyV2SubType subType) {
		fcLoadMap.setInitialDirectory(config().getRecentLoadDir());
		File file = fcLoadMap.showOpenDialog(null);
		if (file != null) {
			try {
				Mappings mappings = new TinyV2Mappings(file.toPath(), controller.getWorkspace(), subType);
				mappings.setCheckFieldHierarchy(true);
				mappings.setCheckMethodHierarchy(true);
				mappings.accept(controller.getWorkspace().getPrimary());
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
			HistoryPane pane = new HistoryPane(controller);
			PluginsManager.getInstance()
					.addPlugin(new HistoryPane.HistoryPlugin(pane));
			stage = controller.windows().window(translate("ui.menubar.history"), pane, 800, 600);
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
			UiUtil.showDocument(URI.create(Recaf.DOC_URL));
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
			UiUtil.showDocument(Recaf.getDirectory("plugins").toUri());
		} catch(IOException ex) {
			Log.error(ex, "Failed to open plugins directory");
		}
	}

	/**
	 * Display attach window.
	 */
	private void attachExisting() {
		Stage stage = controller.windows().getAttachWindow();
		if(stage == null) {
			stage = controller.windows().window(translate("ui.menubar.attach"), new AttachPane(controller), 800, 600);
			controller.windows().setAttachWindow(stage);
		}
		stage.show();
		stage.toFront();
	}

	/**
	 * Display JVM creation window.
	 */
	private void attachCreate() {
		Stage stage = controller.windows().getJvmCreatorWindow();
		if(stage == null) {
			stage = controller.windows().window(translate("ui.createjvm"), new JvmCreationPane(controller), 650, 480);
			controller.windows().setJvmCreatorWindow(stage);
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
			if (Files.isDirectory(fspath))
				name += "/";
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
