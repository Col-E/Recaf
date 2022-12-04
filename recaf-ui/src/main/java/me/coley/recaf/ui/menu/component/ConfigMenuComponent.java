package me.coley.recaf.ui.menu.component;

import javafx.scene.control.Menu;
import me.coley.recaf.ui.menu.MainMenu;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.GenericWindow;

public class ConfigMenuComponent  extends MenuComponent{
	@Override
	protected Menu create(MainMenu mainMenu) {
		return actionMenu("menu.config", Icons.CONFIG, this::openConfig);
	}

	private void openConfig() {
		GenericWindow window = windows.getConfigWindow();
		window.titleProperty().bind(Lang.getBinding("menu.config"));
		window.getStage().setWidth(1080);
		window.getStage().setHeight(600);
		window.show();
		window.requestFocus();
	}
}
