package software.coley.recaf.ui.menubar;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.control.Menu;
import javafx.stage.Stage;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.kordamp.ikonli.javafx.FontIcon;
import software.coley.recaf.services.config.ConfigManager;
import software.coley.recaf.services.window.WindowManager;

import static software.coley.recaf.util.Lang.getBinding;
import static software.coley.recaf.util.Menus.action;

/**
 * Config menu component for {@link MainMenu}.
 *
 * @author Matt Coley
 */
@Dependent
public class ConfigMenu extends Menu {
	private final WindowManager windowManager;
	private final ConfigManager configManager;

	@Inject
	public ConfigMenu(WindowManager windowManager,
					  ConfigManager configManager) {
		this.windowManager = windowManager;
		this.configManager = configManager;

		textProperty().bind(getBinding("menu.config"));
		setGraphic(new FontIcon(CarbonIcons.SETTINGS));

		getItems().add(action("menu.config.edit", CarbonIcons.CALIBRATE, this::openEditor));
		getItems().add(action("menu.config.export", CarbonIcons.DOCUMENT_EXPORT, this::exportProfile));
		getItems().add(action("menu.config.import", CarbonIcons.DOCUMENT_IMPORT, this::importProfile));
	}

	/**
	 * Display the config window.
	 */
	private void openEditor() {
		Stage configWindow = windowManager.getConfigWindow();
		configWindow.show();
		configWindow.requestFocus();
	}

	/**
	 * Exports the current config to a file.
	 */
	private void exportProfile() {
		// TODO: implement
	}

	/**
	 * Applies values in the profile file provided by the user to the current config.
	 */
	private void importProfile() {
		// TODO: implement
	}
}
