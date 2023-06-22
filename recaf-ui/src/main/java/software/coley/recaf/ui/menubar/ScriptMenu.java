package software.coley.recaf.ui.menubar;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableCollection;
import software.coley.recaf.services.script.ScriptEngine;
import software.coley.recaf.services.script.ScriptFile;
import software.coley.recaf.services.script.ScriptManager;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.pane.ScriptManagerPane;
import software.coley.recaf.util.FxThreadUtil;

import java.util.List;
import java.util.TreeSet;

import static software.coley.recaf.util.Lang.getBinding;
import static software.coley.recaf.util.Menus.*;

/**
 * Scripting menu component for {@link MainMenu}.
 *
 * @author Matt Coley
 */
@Dependent
public class ScriptMenu extends Menu {
	private final Menu menuScripts = menu("menu.scripting.list", CarbonIcons.LIST);
	private final WindowManager windowManager;
	private final ObservableCollection<ScriptFile, List<ScriptFile>> scriptFiles;
	private final ObservableBoolean hasScripts;
	private final ScriptManagerPane scriptManagerPane;
	private final ScriptEngine engine;

	@Inject
	public ScriptMenu(WindowManager windowManager, ScriptManager scriptManager, ScriptEngine engine,
					  ScriptManagerPane scriptManagerPane) {
		this.windowManager = windowManager;
		this.scriptManagerPane = scriptManagerPane;
		this.engine = engine;

		scriptFiles = scriptManager.getScriptFiles();
		hasScripts = scriptFiles.mapBoolean(List::isEmpty).negated();

		textProperty().bind(getBinding("menu.scripting"));
		setGraphic(new FontIconView(CarbonIcons.SCRIPT));

		getItems().add(menuScripts);
		getItems().add(action("menu.scripting.manage", CarbonIcons.INFORMATION, this::openManager));
		getItems().add(action("menu.scripting.new", CarbonIcons.ADD_ALT, this::newScript));

		hasScripts.addChangeListener((ob, old, cur) -> refreshList());
		refreshList();
	}

	/**
	 * Update the items in the scripts menu.
	 */
	private void refreshList() {
		FxThreadUtil.run(() -> {
			menuScripts.getItems().clear();
			if (hasScripts.getValue()) {
				for (ScriptFile scriptFile : new TreeSet<>(scriptFiles)) {
					menuScripts.getItems().add(actionLiteral(scriptFile.name(), CarbonIcons.PLAY_FILLED,
							() -> scriptFile.execute(engine)));
				}
			} else {
				MenuItem item = new MenuItem();
				item.setGraphic(new FontIconView(CarbonIcons.SEARCH));
				item.textProperty().bind(getBinding("menu.scripting.none-found"));
				item.setDisable(true);
				menuScripts.getItems().add(item);
			}
		});
	}

	/**
	 * Display the script manager window.
	 */
	private void openManager() {
		Stage scriptsWindow = windowManager.getScriptManagerWindow();
		scriptsWindow.show();
		scriptsWindow.requestFocus();
	}

	/**
	 * Opens a new script editor.
	 */
	private void newScript() {
		scriptManagerPane.newScript();
	}
}
