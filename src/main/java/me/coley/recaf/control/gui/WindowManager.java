package me.coley.recaf.control.gui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import me.coley.recaf.ui.MainWindow;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import static me.coley.recaf.util.ClasspathUtil.*;
import static javafx.scene.input.KeyCode.*;

/**
 * Window manager.
 *
 * @author Matt
 */
public class WindowManager {
	private final GuiController controller;
	private final Set<Stage> windows = new LinkedHashSet<>();
	private MainWindow mainWindow;
	private Stage configWindow;

	WindowManager(GuiController controller) {
		this.controller = controller;
	}

	/**
	 * Create an auto-sized scene for the given content.
	 *
	 * @param title
	 * 		Window title.
	 * @param content
	 * 		Content to fill scene.
	 *
	 * @return Scene with content.
	 */
	public Stage window(String title, Parent content) {
		return window(title, content, -1, -1);
	}

	/**
	 * Create a window with a given size.
	 *
	 * @param title
	 * 		Window title.
	 * @param content
	 * 		Content to fill scene.
	 * @param width
	 * 		Scene width.
	 * @param height
	 * 		Scene height;
	 *
	 * @return Window with content of the given size.
	 */
	public Stage window(String title, Parent content, int width, int height) {
		// Create sized scene
		Scene scene;
		if (width > 0 && height > 0)
			scene = new Scene(content, width, height);
		else
			scene = new Scene(content);
		// Add style-sheets
		reapplyStyle(scene);
		// Create window
		Stage stage = new Stage();
		stage.setScene(scene);
		stage.getIcons().add(new Image(resource("icons/logo.png")));
		stage.setTitle(title);
		// TODO: Global keybinds?
		scene.setOnKeyReleased(e -> {
			if (e.getCode() == ESCAPE)
				stage.close();
		});
		// Active window handling
		stage.setOnShown(e -> {
			stage.requestFocus();
			windows.add(stage);
		});
		stage.setOnCloseRequest(e -> {
			windows.remove(stage);
		});
		return stage;
	}

	/**
	 * Applies the current style to all scenes.
	 */
	public void reapplyStyles() {
		Stream.concat(getWindows().stream(), Stream.of(getMainWindow().getStage(),
				getConfigWindow())).forEach(s -> reapplyStyle(s.getScene()));
	}

	/**
	 * Applies the current style to the given scene.
	 *
	 * @param scene
	 * 		Scene to reapply styles to.
	 */
	public void reapplyStyle(Scene scene) {
		String style = controller.config().display().style;
		String[] fallbacks = new String[]{
			"/style/base.css",
			"/style/ui-default.css",
			"/style/opcodes-default.css",
			"/style/decompile-default.css"};
		String[] paths = new String[]{
			"/style/base.css",
			"/style/ui-" + style + ".css",
			"/style/opcodes-" + style + ".css",
			"/style/decompile-" + style + ".css"};
		scene.getStylesheets().clear();
		for(int i = 0; i < paths.length; i++) {
			String path = paths[i];
			if(resourceExists(path))
				scene.getStylesheets().add(path);
			else
				scene.getStylesheets().add(fallbacks[i]);
		}
	}

	/**
	 * @param window
	 * 		Main Recaf window.
	 */
	public void setMainWindow(MainWindow window) {
		this.mainWindow = window;
	}

	/**
	 * @return Main Recaf window.
	 */
	public MainWindow getMainWindow() {
		return mainWindow;
	}

	/**
	 * @param window
	 * 		Config window
	 */
	public void setConfigWindow(Stage window) {
		this.configWindow = window;
	}

	/**
	 * @return Config window.
	 */
	public Stage getConfigWindow() {
		return configWindow;
	}

	/**
	 * @return Active windows, excluding the {@link #getMainWindow() main window}.
	 */
	public Set<Stage> getWindows() {
		return windows;
	}
}
