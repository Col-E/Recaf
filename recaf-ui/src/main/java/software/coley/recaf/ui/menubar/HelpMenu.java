package software.coley.recaf.ui.menubar;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.control.Menu;
import javafx.stage.Stage;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.util.DesktopUtil;
import software.coley.recaf.util.Icons;

import java.net.URI;

import static software.coley.recaf.util.Lang.getBinding;
import static software.coley.recaf.util.Menus.action;

/**
 * Help menu component for {@link MainMenu}.
 *
 * @author Matt Coley
 */
@Dependent
public class HelpMenu extends Menu {
	private static final Logger logger = Logging.get(HelpMenu.class);
	private final WindowManager windowManager;

	@Inject
	public HelpMenu(@Nonnull WindowManager windowManager) {
		this.windowManager = windowManager;

		textProperty().bind(getBinding("menu.help"));
		setGraphic(new FontIconView(CarbonIcons.HELP));

		getItems().add(action("menu.help.sysinfo", CarbonIcons.INFORMATION, this::openSystem));
		getItems().add(action("menu.help.docs", CarbonIcons.NOTEBOOK_REFERENCE, this::openDocumentation));
		getItems().add(action("menu.help.docsdev", CarbonIcons.NOTEBOOK_REFERENCE, this::openDeveloperDocumentation));
		getItems().add(action("menu.help.github", CarbonIcons.LOGO_GITHUB, this::openGithub));
		getItems().add(action("menu.help.issues", CarbonIcons.LOGO_GITHUB, this::openGithubIssues));
		getItems().add(action("menu.help.discord", Icons.DISCORD, this::openDiscord));
	}

	/**
	 * Display the system information window.
	 */
	private void openSystem() {
		Stage systemWindow = windowManager.getSystemInfoWindow();
		systemWindow.show();
		systemWindow.requestFocus();
	}

	/**
	 * Gotta pump up that member count, right?
	 */
	private void openDiscord() {
		browse("https://discord.gg/Bya5HaA");
	}

	/**
	 * Star pls.
	 */
	private void openGithub() {
		browse("https://github.com/Col-E/Recaf");
	}

	/**
	 * Bugs and features I'll get to <i>"eventually(tm)"</i>
	 */
	private void openGithubIssues() {
		browse("https://github.com/Col-E/Recaf/issues");
	}

	/**
	 * Let's be honest, nobody reads this... <i>Unless we force them to.</i>
	 */
	private void openDocumentation() {
		browse("https://recaf.coley.software/user/index.html");
	}

	/**
	 * Now, if you're reading this one, that's pretty cool.
	 */
	private void openDeveloperDocumentation() {
		browse("https://recaf.coley.software/dev/index.html");
	}


	private static void browse(String uri) {
		try {
			DesktopUtil.showDocument(new URI(uri));
		} catch (Exception ex) {
			logger.error("Failed to open link", ex);
		}
	}
}
