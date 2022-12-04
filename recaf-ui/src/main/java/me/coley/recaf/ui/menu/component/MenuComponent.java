package me.coley.recaf.ui.menu.component;

import javafx.scene.control.Menu;
import me.coley.recaf.ui.menu.MainMenu;
import me.coley.recaf.ui.util.Menus;
import me.coley.recaf.ui.window.Windows;

public abstract class MenuComponent extends Menus {
	protected Windows windows;
	private Menu menu;

	public void setWindows(Windows windows) {
		this.windows = windows;
	}

	public final Menu get(MainMenu mainMenu) {
		if (menu == null) {
			menu = create(mainMenu);
		}
		return menu;
	}

	protected abstract Menu create(MainMenu mainMenu);
}
