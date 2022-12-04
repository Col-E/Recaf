package me.coley.recaf.ui.menu.component;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import me.coley.recaf.scripting.Script;
import me.coley.recaf.scripting.ScriptManager;
import me.coley.recaf.ui.menu.MainMenu;
import me.coley.recaf.ui.pane.ScriptManagerPane;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.GenericWindow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ApplicationScoped
public class ScriptingMenuComponent extends MenuComponent {
	private final Menu menuScripting = menu("menu.scripting", Icons.CODE);
	private final ScriptManagerPane scriptManagerPane;

	@Inject
	public ScriptingMenuComponent(ScriptManager scriptManager,
								  ScriptManagerPane scriptManagerPane) {
		this.scriptManagerPane = scriptManagerPane;
		scriptManager.getScripts().addChangeListener((source, change) -> {
			menuScripting.getItems().clear();
			// When the list of scripts is updated, re-populate the menu
			if (!source.isEmpty()) {
				List<MenuItem> scriptMenuItems = new ArrayList<>();
				for (Script script : source) {
					MenuItem item = new MenuItem(script.getName());
					item.setGraphic(Icons.getIconView(Icons.PLAY));
					item.setOnAction(event -> script.execute());
					scriptMenuItems.add(item);
				}
				repopulate(scriptMenuItems);
			}
		});
	}

	@Override
	protected Menu create(MainMenu mainMenu) {
		return menuScripting;
	}


	/**
	 * Updates the 'Scripting' menu with a list of menu items.
	 *
	 * @param scriptItems
	 * 		The script menu items
	 */
	private void repopulate(Collection<MenuItem> scriptItems) {
		Menu recent = menu("menu.scripting.list", Icons.FILE_TEXT);
		if (scriptItems != null)
			recent.getItems().addAll(scriptItems);
		menuScripting.getItems().add(recent);
		menuScripting.getItems().add(action("menu.scripting.manage", Icons.OPEN_FILE, this::openScriptManager));
		menuScripting.getItems().add(action("menu.scripting.new", Icons.PLUS, scriptManagerPane::createNewScript));
	}

	/**
	 * Open scripts manager window.
	 */
	private void openScriptManager() {
		GenericWindow window = windows.getScriptsWindow();
		window.titleProperty().bind(Lang.getBinding("menu.scripting.manage"));
		window.getStage().setWidth(750);
		window.getStage().setHeight(450);
		window.show();
		window.requestFocus();
	}
}
