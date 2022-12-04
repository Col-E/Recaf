package me.coley.recaf.ui.menu.component;

import javafx.scene.control.Menu;
import me.coley.recaf.ui.menu.MainMenu;
import me.coley.recaf.ui.pane.SearchPane;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.GenericWindow;

public class SearchMenuComponent extends MenuComponent {
	@Override
	protected Menu create(MainMenu mainMenu) {
		Menu menuSearch = menu("menu.search", Icons.ACTION_SEARCH);
		menuSearch.disableProperty().bind(mainMenu.noWorkspaceProperty());
		menuSearch.getItems().add(action("menu.search.string", Icons.QUOTE,
				() -> showSearch("menu.search.string", SearchPane.createTextSearch())));
		menuSearch.getItems().add(action("menu.search.number", Icons.NUMBERS,
				() -> showSearch("menu.search.number", SearchPane.createNumberSearch())));
		menuSearch.getItems().add(action("menu.search.references", Icons.REFERENCE,
				() -> showSearch("menu.search.references", SearchPane.createReferenceSearch())));
		menuSearch.getItems().add(action("menu.search.declarations", Icons.T_STRUCTURE,
				() -> showSearch("menu.search.declarations", SearchPane.createDeclarationSearch())));
		return menuSearch;
	}

	private void showSearch(String key, SearchPane content) {
		GenericWindow window = new GenericWindow(content);
		window.titleProperty().bind(Lang.getBinding(key));
		window.show();
	}
}
