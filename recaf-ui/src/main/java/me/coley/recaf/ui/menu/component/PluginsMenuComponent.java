package me.coley.recaf.ui.menu.component;

import jakarta.enterprise.context.ApplicationScoped;
import javafx.scene.control.Menu;
import me.coley.recaf.ui.menu.MainMenu;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.GenericWindow;

@ApplicationScoped
public class PluginsMenuComponent extends MenuComponent {
	private final Menu menuPlugins = menu("menu.plugin", Icons.PLUGIN);

	@Override
	protected Menu create(MainMenu mainMenu) {
		menuPlugins.getItems().add(action("menu.plugin.manage", Icons.OPEN_FILE, this::openPluginManager));
		return menuPlugins;
	}

	private void openPluginManager() {
		GenericWindow window = windows.getPluginsWindow();
		window.titleProperty().bind(Lang.getBinding("menu.plugin.manage"));
		window.getStage().setWidth(750);
		window.getStage().setHeight(450);
		window.show();
		window.requestFocus();
	}
}
