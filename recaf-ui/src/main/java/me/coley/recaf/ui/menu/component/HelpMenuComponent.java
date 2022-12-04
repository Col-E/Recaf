package me.coley.recaf.ui.menu.component;

import jakarta.enterprise.context.ApplicationScoped;
import javafx.scene.control.Menu;
import me.coley.recaf.ui.menu.MainMenu;
import me.coley.recaf.ui.pane.InfoPane;
import me.coley.recaf.ui.util.Help;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.GenericWindow;

@ApplicationScoped
public class HelpMenuComponent extends MenuComponent{
	@Override
	protected Menu create(MainMenu mainMenu) {
		Menu menuHelp = menu("menu.help", Icons.HELP);
		menuHelp.getItems().add(action("menu.help.sysinfo", Icons.INFO, this::openInfo));
		menuHelp.getItems().add(action("menu.help.docs", Icons.HELP, Help::openDocumentation));
		menuHelp.getItems().add(action("menu.help.github", Icons.GITHUB, Help::openGithub));
		menuHelp.getItems().add(action("menu.help.issues", Icons.GITHUB, Help::openGithubIssues));
		menuHelp.getItems().add(action("menu.help.discord", Icons.DISCORD, Help::openDiscord));
		return menuHelp;
	}

	private void openInfo() {
		GenericWindow window = new GenericWindow(new InfoPane());
		window.titleProperty().bind(Lang.getBinding("menu.help.sysinfo"));
		window.show();
	}
}
