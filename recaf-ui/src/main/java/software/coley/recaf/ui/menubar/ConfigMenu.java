package software.coley.recaf.ui.menubar;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.binding.StringBinding;
import javafx.scene.control.Menu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
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
import java.util.ArrayList;
import java.util.List;

import static software.coley.recaf.util.Lang.getBinding;
import static software.coley.recaf.util.Menus.action;
import static software.coley.recaf.util.Menus.menu;

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
	private final Menu profileMenu;

	@Inject
	public ConfigMenu(WindowManager windowManager,
					  ConfigManager configManager) {
		this.windowManager = windowManager;
		this.configManager = configManager;

		textProperty().bind(getBinding("menu.config"));
		setGraphic(new FontIcon(CarbonIcons.SETTINGS));
		setOnShowing(event -> refreshProfileMenu());

		profileMenu = createProfileMenu();

		getItems().add(action("menu.config.edit", CarbonIcons.CALIBRATE, this::openEditor));
		getItems().add(profileMenu);
		getItems().add(action("menu.config.export", CarbonIcons.DOCUMENT_EXPORT, this::exportProfile));
		getItems().add(action("menu.config.import", CarbonIcons.DOCUMENT_IMPORT, this::importProfile));
	}

	/**
	 * @return Menu for switching between config profiles.
	 */
	@Nonnull
	private Menu createProfileMenu() {
		Menu profileMenu = menu("menu.config.profile", CarbonIcons.COLLABORATE);
		refreshProfileMenu(profileMenu);
		return profileMenu;
	}

	/**
	 * Refreshes the profile menu with the current profiles and active profile.
	 */
	private void refreshProfileMenu() {
		refreshProfileMenu(profileMenu);
	}

	/**
	 * Refreshes the profile menu with the current profiles and active profile.
	 *
	 * @param profileMenu
	 * 		Menu to refresh.
	 */
	private void refreshProfileMenu(@Nonnull Menu profileMenu) {
		profileMenu.getItems().clear();

		// Collect profile names and active profile.
		String activeProfile;
		List<String> profileNames;
		try {
			activeProfile = configManager.ensureActiveProfile();
			profileNames = new ArrayList<>(configManager.getProfileNames());
		} catch (IOException ex) {
			logger.error("Failed to load config profiles", ex);
			showProfileError(
					getBinding("dialog.error.importconfig.title"),
					getBinding("dialog.error.importconfig.header"),
					getBinding("dialog.error.importconfig.content"),
					ex
			);
			return;
		}

		// Create radio menu items for each profile, with the active profile selected.
		ToggleGroup toggleGroup = new ToggleGroup();
		for (String profileName : profileNames) {
			RadioMenuItem item = new RadioMenuItem(profileName);
			item.setToggleGroup(toggleGroup);
			item.setSelected(profileName.equals(activeProfile));
			item.setOnAction(event -> switchProfile(profileName));
			profileMenu.getItems().add(item);
		}
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
		if (!exportPath.getFileName().toString().toLowerCase().endsWith(".zip"))
			exportPath = exportPath.resolveSibling(exportPath.getFileName() + ".zip");

		try {
			configManager.exportProfileTo(exportPath);
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
			configManager.importProfileFrom(importPath);
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

	private void switchProfile(@Nonnull String profileName) {
		String normalizedProfile = ConfigManager.normalizeProfileName(profileName);
		String currentProfile = ConfigManager.normalizeProfileName(configManager.getServiceConfig().getCurrentProfile().getValue());
		if (normalizedProfile.equals(currentProfile))
			return;

		try {
			configManager.exportProfile(currentProfile);
			configManager.importProfile(normalizedProfile);
			configManager.getServiceConfig().getCurrentProfile().setValue(normalizedProfile);
			logger.info("Switched active config profile from '{}' to '{}'", currentProfile, normalizedProfile);
		} catch (IOException ex) {
			logger.error("Failed to switch config profile to '{}'", normalizedProfile, ex);
			showProfileError(
					getBinding("dialog.error.importconfig.title"),
					getBinding("dialog.error.importconfig.header"),
					getBinding("dialog.error.importconfig.content"),
					ex
			);
		}
	}

	private void showProfileError(@Nonnull StringBinding title,
								  @Nonnull StringBinding header,
								  @Nonnull StringBinding content,
								  @Nonnull Throwable throwable) {
		ErrorDialogs.show(title, header, content, throwable);
	}
}
