package me.coley.recaf.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.recaf.Input;
import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.Agent;
import me.coley.recaf.config.impl.*;
import me.coley.recaf.event.*;
import me.coley.recaf.plugin.Plugins;
import me.coley.recaf.plugin.Stageable;
import me.coley.recaf.ui.component.*;
import me.coley.recaf.util.*;

import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.*;

/**
 * Primary window.
 * 
 * @author Matt
 */
public class FxWindow extends Application {
	// UI components
	private Stage stage;
	private Menu menuSearch, menuHistory, menuRecent;
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
		Runnable rAbout = () -> FxAbout.open();
		Runnable rDocumentation = () -> {
			try {
				Desktop.getDesktop().browse(new URI("https://col-e.github.io/Recaf/documentation.html"));
			} catch(Exception e) {
				Logging.error("Failed to open documentation URL");
			}
		};
		// Menubar
		Menu menuFile = new Menu(Lang.get("ui.menubar.file"));
		menuFile.getItems().add(new ActionMenuItem(Lang.get("ui.menubar.load"), rLoad));
		menuFile.getItems().add(new ActionMenuItem(Lang.get("ui.menubar.export"), rExport));
		if (Agent.isActive()) {
			menuFile.getItems().add(new ActionMenuItem(Lang.get("ui.menubar.agentexport"), rAgentSave));
		}
		menuRecent = new Menu(Lang.get("ui.menubar.recent"));
		menuFile.getItems().add(menuRecent);
		updateRecent();
		Menu menuConfig = new ActionMenu(Lang.get("ui.menubar.config"), rConfig);
		menuSearch = new ActionMenu(Lang.get("ui.menubar.search"), rSearch);
		menuSearch.setDisable(true);
		menuHistory = new Menu(Lang.get("ui.menubar.history"));
		menuHistory.setDisable(true);
		menuHistory.getItems().add(new ActionMenuItem(Lang.get("ui.menubar.history.new"), rSave));
		menuHistory.getItems().add(new ActionMenuItem(Lang.get("ui.menubar.history.view"), rHistory));
		Menu menuAttach = new ActionMenu(Lang.get("ui.menubar.attach"), rAttach);
		Menu menuPlugins = new Menu(Lang.get("ui.menubar.plugins"));
		Menu menuHelp = new Menu(Lang.get("ui.menubar.help"));
		menuHelp.getItems().add(new ActionMenuItem(Lang.get("ui.menubar.help.about"), rAbout));
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			menuHelp.getItems().add(new ActionMenuItem(Lang.get("ui.menubar.help.documentation"), rDocumentation));
		}
		MenuBar menubar = new MenuBar(menuFile, menuSearch, menuConfig, menuHistory, menuHelp);
		menubar.getStyleClass().add("ui-menu-bar");
		if (Misc.canAttach() && !Agent.isActive()) {
			// only add if it is offered by the runtime and is not currrently running as an agent
			menubar.getMenus().add(menuAttach);
		}
		Threads.runFx(() -> {
			Collection<Stageable> plugins = Plugins.instance().plugins(Stageable.class);
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
		SplitPane vertical = new SplitPane(new ClassTabs(), tabInfo);
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
		BorderPane borderPane = new BorderPane();
		borderPane.setTop(top);
		borderPane.setCenter(horizontal);
		Scene scene = JavaFX.scene(borderPane, ScreenUtil.prefWidth(), ScreenUtil.prefHeight());
		stage.setOnCloseRequest(we -> {
			// warn user that they are about to exit
			if (ConfDisplay.instance().warnOnExit && Input.get() != null) {
				Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
				alert.setTitle(Lang.get("display.exitwarning.title"));
				alert.setContentText(Lang.get("display.exitwarning.mesage"));
				Optional<ButtonType> result = alert.showAndWait();
				if (result.get() == ButtonType.OK){
					FileChoosers.export();
				}
			}
			// closing the primary stage should exit the program
			if (Agent.isActive()) {
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
		ConfKeybinds.instance().registerStage(stage);
		this.stage = stage;
		// post notification of completion
		Bus.post(new UiInitEvent(getParameters()));
		Bus.subscribe(this);
	}

	/**
	 * Update the recently loaded list. Priority is higher to indicate it is
	 * called later than the default priority (0).
	 * 
	 * @param input
	 */
	@Listener(priority = 1)
	private void onImport(NewInputEvent input) {
		updateRecent();
	}

	/**
	 * Update the recently loaded list. 
	 */
	private void updateRecent() {
		boolean empty = ConfOther.instance().recent.isEmpty();
		menuRecent.setDisable(empty);
		menuRecent.getItems().clear();
		// Populate recent item entries
		if (!empty) {
			for (String filePath : new ArrayList<>(ConfOther.instance().recent)) {
				File file = new File(filePath);
				if (!file.exists()) {
					ConfOther.instance().recent.remove(filePath);
					continue;
				}
				String name = filePath;
				int l = name.length();
				int max = 50;
				if (l > max) {
					name = "..." + name.substring(l - max, l);
				}
				MenuItem mniFile = new ActionMenuItem(name, () -> NewInputEvent.call(file));
				menuRecent.getItems().add(mniFile);
			}
		}
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
	 * Entry point. Explicitly declared because invoking launch from another
	 * class doesn't fly well with JavaFX.
	 * 
	 * @param args
	 */
	public static void init(String[] args) {
		try {
		    launch(args);
		} catch (IllegalStateException e) {
			// If recaf tries to connect to a JavaFX application, the
			// Application.launch() will fail.
			// Since the platform is already running though, we can simply throw
			// up a new stage.
			Threads.runFx(() -> {
				FxWindow window = new FxWindow();
				Stage stage = new Stage();
				window.start(stage);
				stage.show();
			});
		}
	}
}
