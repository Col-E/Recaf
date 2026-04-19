package software.coley.recaf.ui.menubar;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.control.Menu;
import javafx.stage.Stage;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.config.ConfigManager;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.util.ErrorDialogs;
import software.coley.recaf.util.FileChooserBuilder;
import software.coley.recaf.util.Lang;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

import static software.coley.recaf.util.Lang.getBinding;
import static software.coley.recaf.util.Menus.action;

/**
 * Config menu component for {@link MainMenu}.
 *
 * @author Matt Coley
 */
@Dependent
public class ConfigMenu extends Menu {
	private static final Logger logger = Logging.get(ConfigMenu.class);
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
		File selectedFile = new FileChooserBuilder()
				.setInitialFileName("recaf-config.zip")
				.setFileExtensionFilter("ZIP", "*.zip")
				.setTitle(Lang.get("dialog.file.export"))
				.save(windowManager.getMainWindow());
		if (selectedFile == null)
			return;

		Path exportPath = selectedFile.toPath();
		if (!exportPath.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
			exportPath = exportPath.resolveSibling(exportPath.getFileName() + ".zip");

		try {
			configManager.exportProfile(exportPath);
			logger.info("Exported config profile to path '{}'", exportPath);
		} catch (IOException ex) {
			logger.error("Failed to export config profile to path '{}'", exportPath, ex);
			ErrorDialogs.show(
					getBinding("dialog.error.exportconfig.title"),
					getBinding("dialog.error.exportconfig.header"),
					getBinding("dialog.error.exportconfig.content"),
					ex
			);
		}
	}

	/**
	 * Applies values in the profile file provided by the user to the current config.
	 */
	private void importProfile() {
		File selectedFile = new FileChooserBuilder()
				.setFileExtensionFilter("ZIP", "*.zip")
				.setTitle(Lang.get("dialog.file.open"))
				.open(windowManager.getMainWindow());
		if (selectedFile == null)
			return;

		Path importPath = selectedFile.toPath();
		try {
			configManager.importProfile(importPath);
			logger.info("Imported config profile from path '{}'", importPath);
		} catch (IOException ex) {
			logger.error("Failed to import config profile from path '{}'", importPath, ex);
			ErrorDialogs.show(
					getBinding("dialog.error.importconfig.title"),
					getBinding("dialog.error.importconfig.header"),
					getBinding("dialog.error.importconfig.content"),
					ex
			);
		}
	}
}
