package me.coley.recaf.ui.control.config;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import me.coley.recaf.RecafUI;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.ui.control.ActionButton;
import me.coley.recaf.util.ReflectUtil;

import java.io.File;
import java.lang.reflect.Field;

/**
 * Component for {@code String} config values indicating a {@link java.nio.file.Path}.
 *
 * @author Matt Coley
 */
public class ConfigPath extends BorderPane {
	private final TextField txtPath = new TextField();

	/**
	 * @param instance
	 * 		Config container.
	 * @param field
	 * 		Config field.
	 */
	public ConfigPath(ConfigContainer instance, Field field) {
		Button btnSelectPath = new ActionButton("...", this::choose);
		txtPath.setEditable(false);
		txtPath.setText(ReflectUtil.quietGet(instance, field));
		txtPath.textProperty().addListener((observable, old, current) -> ReflectUtil.quietSet(instance, field, current));
		setCenter(txtPath);
		setRight(btnSelectPath);
	}

	private void choose() {
		File directory = new File(txtPath.getText());
		if (directory.isFile()) {
			directory = directory.getParentFile();
		} else if (!directory.exists()) {
			directory = new File(System.getProperty("user.dir"));
		}
		FileChooser chooser = new FileChooser();
		chooser.setInitialDirectory(directory);
		decorate(chooser);
		File file = chooser.showOpenDialog(RecafUI.getWindows().getConfigWindow());
		if (file != null && file.isFile()) {
			txtPath.setText(file.getAbsolutePath());
		}
	}

	protected void decorate(FileChooser chooser) {
		// Implemented by child-type
	}
}
