package me.coley.recaf.ui;

import java.util.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.stage.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import me.coley.event.*;
import me.coley.recaf.config.impl.*;
import me.coley.recaf.event.*;
import me.coley.recaf.plugins.Plugins;
import me.coley.recaf.plugins.Stageable;
import me.coley.recaf.ui.component.*;
import me.coley.recaf.util.*;

public class FxWindow extends Application {
	private Stage stage;
	private Menu menuSearch, menuHistory;
	private Button btnExport, btnSaveState, btnSearch;

	@Override
	public void start(Stage stage) {
		// Actions
		Runnable rExport = () -> FileChoosers.export();
		Runnable rLoad = () -> FileChoosers.open();
		Runnable rSave = () -> Bus.post(new RequestSaveStateEvent());
		Runnable rAgentSave = () -> Bus.post(new RequestAgentSaveEvent());
		Runnable rSearch = () -> FxSearch.open();
		Runnable rConfig = () -> FxConfig.open();
		Runnable rHistory = () -> FxHistory.open();
		Runnable rAttach = () -> FxAttach.open();
		BorderPane borderPane = new BorderPane();
		// Menubar
		Menu menuFile = new Menu(Lang.get("ui.menubar.file"));
		menuFile.getItems().add(new ActionMenuItem(Lang.get("ui.menubar.load"), rLoad));
		menuFile.getItems().add(new ActionMenuItem(Lang.get("ui.menubar.export"), rExport));
		if (isAgent()) {
			menuFile.getItems().add(new ActionMenuItem(Lang.get("ui.menubar.agentexport"), rAgentSave));
		}
		Menu menuConfig = new ActionMenu(Lang.get("ui.menubar.config"), rConfig);
		menuSearch = new ActionMenu(Lang.get("ui.menubar.search"), rSearch);
		menuSearch.setDisable(true);
		menuHistory = new Menu(Lang.get("ui.menubar.history"));
		menuHistory.setDisable(true);
		menuHistory.getItems().add(new ActionMenuItem(Lang.get("ui.menubar.history.new"), rSave));
		menuHistory.getItems().add(new ActionMenuItem(Lang.get("ui.menubar.history.view"), rHistory));
		Menu menuAttach = new ActionMenu(Lang.get("ui.menubar.attach"), rAttach);
		Menu menuPlugins = new Menu(Lang.get("ui.menubar.plugins"));
		MenuBar menubar = new MenuBar(menuFile, menuSearch, menuConfig, menuHistory);
		menubar.getStyleClass().add("ui-menu-bar");
		if (Misc.canAttach()) {
			// only add if it is offered by the runtime
			menubar.getMenus().add(menuAttach);
		}
		Threads.runFx(() -> {
			Collection<Stageable> plugins = Plugins.getStageables();
			if (plugins.size() > 0) {
				// only add if there are plugins
				plugins.forEach(pl -> menuPlugins.getItems().add(pl.createMenuItem()));
				menubar.getMenus().add(menuPlugins);
			}
		});
		// Toolbar (easy access menu-bar)
		Button btnNew = new ToolButton(Icons.T_LOAD, rLoad);
		btnExport = new ToolButton(Icons.T_EXPORT, rExport);
		btnSaveState = new ToolButton(Icons.T_SAVE, rSave);
		btnSearch = new ToolButton(Icons.T_SEARCH, rSearch);
		Button btnConfig = new ToolButton(Icons.T_CONFIG, rConfig);
		btnExport.setDisable(true);
		btnSaveState.setDisable(true);
		btnSearch.setDisable(true);
		btnNew.setTooltip(new Tooltip(Lang.get("ui.menubar.load")));
		btnSaveState.setTooltip(new Tooltip(Lang.get("ui.menubar.history.new")));
		btnSearch.setTooltip(new Tooltip(Lang.get("ui.menubar.search")));
		btnExport.setTooltip(new Tooltip(Lang.get("ui.menubar.export")));
		btnConfig.setTooltip(new Tooltip(Lang.get("ui.menubar.config")));
		ToolBar toolbar = new ToolBar(btnNew, btnExport, btnSaveState, btnSearch, btnConfig);
		toolbar.getStyleClass().add("ui-tool-bar");
		// Info tab
		TabPane tabInfo = new TabPane();
		tabInfo.getStyleClass().add("info-tabs");
		Tab tab = new Tab(Lang.get("ui.info.logging"));
		tab.closableProperty().set(false);
		tab.setContent(new LoggingPane());
		tabInfo.getTabs().add(tab);
		tab = new Tab(Lang.get("ui.info.other"));
		tab.closableProperty().set(false);
		tab.setContent(new BorderPane());
		// tabInfo.getTabs().add(tab);
		// Organization
		SplitPane vertical = new SplitPane(new ClassEditPane(), tabInfo);
		SplitPane horizontal = new SplitPane(new FileTreePane(), vertical);
		horizontal.setDividerPositions(0.25);
		vertical.setDividerPositions(0.777);
		vertical.setOrientation(Orientation.VERTICAL);
		VBox top = new VBox();
		top.setPadding(new Insets(0, 0, 0, 0));
		top.setSpacing(0);
		top.getChildren().add(menubar);
		if (ConfDisplay.instance().toolbar) {
			top.getChildren().add(toolbar);
		}
		borderPane.setTop(top);
		borderPane.setCenter(horizontal);
		Scene scene = JavaFX.scene(borderPane, ScreenUtil.prefWidth(), ScreenUtil.prefHeight());
		stage.setOnCloseRequest(we -> {
			// closing the primary stage should exit the program
			if (isAgent()) {
				// only exit the javafx platform, the targeted process should
				// still be allowed to run
				Platform.exit();
			} else {
				// kill independent process
				System.exit(0);
			}

		});
		stage.setTitle("Recaf");
		stage.getIcons().add(Icons.LOGO);
		stage.setScene(scene);
		stage.show();
		stage.addEventHandler(KeyEvent.KEY_RELEASED, (KeyEvent e) -> {
			// Only continue of control is held
			ConfKeybinds keys = ConfKeybinds.instance();
			if (keys.active && !e.isControlDown()) {
				return;
			}
			String code = e.getCode().getName();
			if (code.equals(keys.save.toUpperCase())) {
				rSave.run();
			} else if (code.equals(keys.export.toUpperCase())) {
				if (isAgent()) {
					rAgentSave.run();
				} else {
					rExport.run();
				}
			} else if (code.equals(keys.open.toUpperCase())) {
				rLoad.run();
			} else if (code.equals(keys.search.toUpperCase())) {
				rSearch.run();
			}
		});
		this.stage = stage;
		// post notification of completion
		Bus.post(new UiInitEvent(getParameters()));
		Bus.subscribe(this);
	}

	@Listener
	private void onInputChange(NewInputEvent event) {
		// Enable menu items when an input is loaded
		menuHistory.setDisable(false);
		menuSearch.setDisable(false);
		btnExport.setDisable(false);
		btnSaveState.setDisable(false);
		btnSearch.setDisable(false);
	}

	@Listener
	private void onTitleChange(TitleChangeEvent event) {
		stage.setTitle(event.getTitle());
	}
	
	public Stage getStage() {
		return stage;
	}

	/**
	 * @return Command line args indicate client invoked as an agent.
	 */
	private boolean isAgent() {
		List<String> jfxArgs = getParameters().getRaw();
		return jfxArgs.contains("-a") || jfxArgs.contains("--agent");
	}

	/**
	 * Entry point. Explicitly declared because invoking launch from another
	 * class doesn't fly well with JavaFX.
	 * 
	 * @param args
	 */
	public static void init(String[] args) {
		launch(args);
	}
}
