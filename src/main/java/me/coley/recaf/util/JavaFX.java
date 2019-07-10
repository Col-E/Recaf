package me.coley.recaf.util;

import java.util.Optional;

import com.sun.javafx.runtime.VersionInfo;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import me.coley.recaf.config.impl.ConfDisplay;
import me.coley.recaf.config.impl.ConfKeybinds;

/**
 * JavaFX utilities.
 * 
 * @author Matt
 */
public class JavaFX {
	/**
	 * See: {@link #isToolkitLoaded()}
	 */
	private static boolean toolkitLoaded;

	/**
	 * Create JavaFX scene with some default attributes.
	 * 
	 * @param node
	 *            Content to fill scene.
	 * @param width
	 *            Scene width.
	 * @param height
	 *            Scene height;
	 * @return Styled scene with defined information.
	 */
	public static Scene scene(Parent node, int width, int height) {
		Scene scene = new Scene(node, width, height);
		String style = ConfDisplay.instance().style;
		//@formatter:off
		String[] fallbacks = new String[] { 
			"resources/style/common-flat.css",
			"resources/style/opcodes-flat.css",
			"resources/style/decompile-flat.css"};
		String[] paths = new String[] { 
			"resources/style/common-" + style + ".css",
			"resources/style/opcodes-" + style + ".css",
			"resources/style/decompile-" + style + ".css"};
		//@formatter:on
		for (int i = 0; i < paths.length; i++) {
			String path = paths[i];
			if (FileIO.resourceExists(path)) {
				scene.getStylesheets().add(path);
			} else {
				scene.getStylesheets().add(fallbacks[i]);
			}

		}
		return scene;
	}

	/**
	 * Create JavaFX stage with some default attributes.
	 * 
	 * @param scene
	 *            Scene to take stage viewport.
	 * @param title
	 *            Stage title.
	 * @param top
	 *            If stage should be <i>checked<i> for topmost.
	 * @return Stage with default attributes.
	 */
	public static Stage stage(Scene scene, String title, boolean top) {
		Stage stage = new Stage();
		stage.setScene(scene);
		stage.getIcons().add(Icons.LOGO);
		stage.setTitle(title);
		stage.setOnShown(e -> {
			stage.requestFocus();
		});
		if (top) {
			stage.setAlwaysOnTop(ConfDisplay.instance().topmost);
			stage.addEventHandler(KeyEvent.KEY_RELEASED, (KeyEvent e) -> {
				if (KeyCode.ESCAPE == e.getCode()) {
					stage.close();
				}
			});
			ConfKeybinds.instance().registerStage(stage);
		}
		return stage;
	}

	/**
	 * Wrap some value in an optional & observable value.
	 * 
	 * @param value
	 *            Value to wrap.
	 * @return Wrapped value.
	 */
	public static <T> Optional<ObservableValue<? extends T>> optionalObserved(T value) {
		return optional(observable(value));
	}

	/**
	 * Wrap some value in an observable value.
	 * 
	 * @param value
	 *            Value to wrap.
	 * @return Wrapped value.
	 */
	public static <T> ObservableValue<T> observable(T value) {
		return new ReadOnlyObjectWrapper<>(value);
	}

	/**
	 * Wrap some array of values in an obvservable list.
	 * 
	 * @param values
	 *            Values to wrap.
	 * @return List wrapper.
	 */
	@SafeVarargs
	public static <T> ObservableList<T> observableList(T... values) {
		return FXCollections.observableArrayList(values);
	}

	/**
	 * Wrap some value in an optional value.
	 * 
	 * @param value
	 *            Value to wrap.
	 * @return Wrapped value.
	 */
	public static <T> Optional<T> optional(T value) {
		return Optional.of(value);
	}

	/**
	 * @return JavaFX version.
	 */
	public static String version() {
		return VersionInfo.getRuntimeVersion();
	}

	/**
	 * @return {@code true} if JavaFX has been initialized. {@code false}
	 *         otherwise.
	 */
	public static boolean isToolkitLoaded() {
		if (toolkitLoaded) return true;
		try {
			Platform.runLater(() -> toolkitLoaded = true);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}