package me.coley.recaf.ui.menu;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.coley.recaf.ui.menu.component.*;
import me.coley.recaf.ui.window.Windows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class MenuBarPopulation {
	private final List<MenuComponent> components = new ArrayList<>();

	@Inject
	public MenuBarPopulation(MainMenu mainMenu,
							 FileMenuComponent fileMenuComponent,
							 ConfigMenuComponent configMenuComponent,
							 SearchMenuComponent searchMenuComponent,
							 MappingsMenuComponent mappingsMenuComponent,
							 ScriptingMenuComponent scriptingMenuComponent,
							 PluginsMenuComponent pluginsMenuComponent) {
		components.addAll(Arrays.asList(
				fileMenuComponent,
				configMenuComponent,
				searchMenuComponent,
				mappingsMenuComponent,
				scriptingMenuComponent,
				pluginsMenuComponent
		));
		components.forEach(c -> mainMenu.addMenu(c.get(mainMenu)));
	}

	public void setWindows(Windows windows) {
		components.forEach(c -> c.setWindows(windows));
	}
}
