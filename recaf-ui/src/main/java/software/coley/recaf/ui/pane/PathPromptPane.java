package software.coley.recaf.ui.pane;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.Spacer;
import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.ui.config.RecentFilesConfig;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.util.DirectoryChooserBuilder;
import software.coley.recaf.util.FileChooserBuilder;
import software.coley.recaf.util.Lang;

import java.io.File;
import java.nio.file.Path;

/**
 * Pane for prompting users for a {@link Path} to a file or directory.
 *
 * @author Matt Coley
 */
public class PathPromptPane extends BorderPane {
	private final ObjectProperty<Path> path = new SimpleObjectProperty<>();
	private final BooleanProperty isFile = new SimpleBooleanProperty(true);

	/**
	 * @param recentFilesConfig
	 * 		Config to pull recent locations from, to populate initial directories of file/directory choosers.
	 */
	public PathPromptPane(@Nonnull RecentFilesConfig recentFilesConfig) {
		// Graphic to indicate if input path is a file or directory.
		FontIconView iconFile = new FontIconView(CarbonIcons.DOCUMENT);
		FontIconView iconDirectory = new FontIconView(CarbonIcons.FOLDER);

		// Text to show the current file name.
		CustomTextField pathField = new CustomTextField(null);
		pathField.leftProperty().bind(isFile.map(is -> is ? iconFile : iconDirectory));
		pathField.promptTextProperty().bind(Lang.getBinding("dialog.file.nothing"));
		pathField.setEditable(false);
		pathField.setMouseTransparent(true);
		pathField.setFocusTraversable(false);
		pathField.getStyleClass().add(Styles.RIGHT);
		pathField.textProperty().bind(path.map(p -> p == null ? null : p.getFileName().toString()));
		isFile.addListener((obs, old, cur) -> path.set(null));

		// Button to prompt user to select a file/directory to load.
		ObservableValue<String> openText = isFile.map(is -> Lang.get("dialog.file.open") + " - " +
				(is ? Lang.get("dialog.file.open.file") : Lang.get("dialog.file.open.directory")));
		Button openButton = new ActionButton(openText, () -> {
			File recentOpenDir = recentFilesConfig.getLastWorkspaceOpenDirectory().unboxingMap(File::new);
			if (isFile.get()) {
				FileChooser chooser = new FileChooserBuilder()
						.setInitialDirectory(recentOpenDir)
						.setTitle(Lang.get("dialog.file.open"))
						.build();

				// Show the prompt, update the path when complete
				File file = chooser.showOpenDialog(getScene().getWindow());
				if (file != null) {
					String parent = file.getParent();
					if (parent != null) recentFilesConfig.getLastWorkspaceOpenDirectory().setValue(parent);
					path.set(file.toPath());
				}
			} else {
				DirectoryChooser chooser = new DirectoryChooserBuilder()
						.setInitialDirectory(recentOpenDir)
						.setTitle(Lang.get("dialog.file.open"))
						.build();

				// Show the prompt, update the path when complete
				File file = chooser.showDialog(getScene().getWindow());
				if (file != null) {
					String parent = file.getParent();
					if (parent != null) recentFilesConfig.getLastWorkspaceOpenDirectory().setValue(parent);
					path.set(file.toPath());
				}
			}
		});
		openButton.setMinWidth(140); // Prevent jolting when text changes

		// Switch to toggle between handling files and directories.
		ToggleSwitch fileDirectoryToggle = new ToggleSwitch();
		fileDirectoryToggle.setSelected(isFile.get());
		fileDirectoryToggle.getStyleClass().add(Styles.LEFT_PILL);
		isFile.bind(fileDirectoryToggle.selectedProperty());
		HBox rightWrapper = new HBox(fileDirectoryToggle, openButton);
		rightWrapper.setAlignment(Pos.CENTER_LEFT);
		rightWrapper.setSpacing(16);

		// Layout
		HBox wrapper = new HBox(pathField, new Spacer(50), rightWrapper);
		HBox.setHgrow(pathField, Priority.ALWAYS);
		wrapper.setFillHeight(false);
		wrapper.setAlignment(Pos.TOP_LEFT);
		setCenter(wrapper);
	}

	/**
	 * @return Selected path. Initially {@code null}.
	 */
	public ObjectProperty<Path> pathProperty() {
		return path;
	}
}
