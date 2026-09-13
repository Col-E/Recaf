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
import software.coley.recaf.ui.pane.PluginManagerPane;
import software.coley.recaf.util.DesktopUtil;

import java.net.URI;

import static software.coley.recaf.util.Lang.getBinding;
import static software.coley.recaf.util.Menus.action;

/**
 * Plugin menu component for {@link MainMenu}.
 *
 * @author Canrad
 * @see PluginManagerPane The manager display this menu links to.
 */
@Dependent
public class PluginMenu extends Menu {
	private static final Logger logger = Logging.get(PluginMenu.class);
	private final WindowManager windowManager;

	@Inject
	public PluginMenu(@Nonnull WindowManager windowManager) {
		this.windowManager = windowManager;

		textProperty().bind(getBinding("menu.plugin"));
		setGraphic(new FontIconView(CarbonIcons.PLUG));

		// Browsing the plugin directory lives inside the manager panel, so the menu only opens the panel + doc links.
		getItems().add(action("menu.plugin.manage", CarbonIcons.SETTINGS_ADJUST, this::openManager));
		getItems().add(action("menu.plugin.devguide", CarbonIcons.NOTEBOOK_REFERENCE, this::openDevGuide));
		getItems().add(action("menu.plugin.template", CarbonIcons.LOGO_GITHUB, this::openTemplate));
	}

	/**
	 * Display the plugin manager window.
	 */
	private void openManager() {
		Stage pluginWindow = windowManager.getPluginManagerWindow();
		pluginWindow.show();
		pluginWindow.requestFocus();
	}

	/**
	 * Opens the online plugin development guide.
	 */
	private void openDevGuide() {
		browseUrl(PluginManagerPane.URL_DEV_GUIDE);
	}

	/**
	 * Opens the template workspace for starting a new plugin project.
	 */
	private void openTemplate() {
		browseUrl(PluginManagerPane.URL_TEMPLATE_WORKSPACE);
	}

	private static void browseUrl(@Nonnull String uri) {
		try {
			DesktopUtil.showDocument(new URI(uri));
		} catch (Exception ex) {
			logger.error("Failed to open link: {}", uri, ex);
		}
	}
}
