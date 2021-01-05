package me.coley.recaf.control.gui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import me.coley.recaf.ui.MainWindow;
import me.coley.recaf.util.Resource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static me.coley.recaf.util.ClasspathUtil.*;
import static me.coley.recaf.ui.controls.FontSlider.addFontSizeStyleSheet;
import static me.coley.recaf.ui.controls.FontComboBox.addMonoFontStyleSheet;


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
	private Stage themeEditorWindow;
	private Stage attachWindow;
	private Stage jvmCreatorWindow;
	private Stage historyWindow;
	private Stage informationWindow;
	private Stage contactWindow;
	private Stage pluginsWindow;

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
	 * 		Scene height.
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
		controller.config().keys().registerWindowKeys(controller, stage, scene);
		// Active window handling
		stage.setOnShown(e -> {
			stage.requestFocus();
			windows.add(stage);
		});
		stage.setOnCloseRequest(e -> {
			windows.remove(stage);
		});
		// Set window owner to main window
		if (mainWindow != null && Screen.getScreens().size() > 1) {
			stage.initOwner(mainWindow.getStage());
			stage.setAlwaysOnTop(false);
		}
		return stage;
	}

	/**
	 * Applies the current style to all scenes.
	 */
	public void reapplyStyles() {
		List<Stage> windows = new ArrayList<>(getWindows());
		windows.add(getMainWindow().getStage());
		windows.add(getConfigWindow());
		windows.add(getAttachWindow());
		windows.add(getThemeEditorWindow());
		windows.forEach(s -> {
			if(s != null)
				reapplyStyle(s.getScene());
		});
	}

	/**
	 * Applies the current style to the given scene.
	 *
	 * @param scene
	 * 		Scene to reapply styles to.
	 */
	public void reapplyStyle(Scene scene) {
		Resource appStyle = controller.config().display().appStyle;
		Resource textStyle = controller.config().display().textStyle;
		Resource[] fallbacks = new Resource[]{
			Resource.internal("/style/base.css"),
			Resource.internal("/style/ui-default.css"),
			Resource.internal("/style/text-default.css")};
		Resource[] paths = new Resource[]{
			fallbacks[0],
			appStyle,
			textStyle
		};
		// Clear, then reapply sheets
		scene.getStylesheets().clear();
		for(int i = 0; i < paths.length; i++) {
			Resource resource = paths[i];
			if(resource.isInternal()) {
				// Load internal stylesheet
				String path = resource.getPath();
				if(resourceExists(path))
					scene.getStylesheets().add(path);
				else
					scene.getStylesheets().add(fallbacks[i].getPath());
			} else {
				// Load external stylesheet
				try {
					URL url = resource.getURL();
					if(new File(url.getFile()).exists())
						scene.getStylesheets().add(url.toString());
					else
						throw new IOException("Failed to load CSS: " + url);
				} catch(IOException ex) {
					scene.getStylesheets().add(fallbacks[i].getPath());
				}
			}
		}
		addFontSizeStyleSheet(scene);
		addMonoFontStyleSheet(scene);
	}

	/**
	 * @return Active windows, excluding the {@link #getMainWindow() main window}.
	 */
	public Set<Stage> getWindows() {
		return windows;
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
	 * @return Theme editor window.
	 */
	public Stage getThemeEditorWindow() {
		return themeEditorWindow;
	}

	/**
	 * @param window
	 * 		Theme editor window
	 */
	public void setThemeEditorWindow(Stage window) {
		this.themeEditorWindow = window;
	}

	/**
	 * @return Config window.
	 */
	public Stage getConfigWindow() {
		return configWindow;
	}

	/**
	 * @param window
	 * 		Config window
	 */
	public void setConfigWindow(Stage window) {
		this.configWindow = window;
	}

	/**
	 * @param window
	 * 		Attach window.
	 */
	public void setAttachWindow(Stage window) {
		this.attachWindow = window;
	}

	/**
	 * @return Attach window.
	 */
	public Stage getAttachWindow() {
		return attachWindow;
	}

	/**
	 * @param window
	 * 		JVM creator window.
	 */
	public void setJvmCreatorWindow(Stage window) {
		this.jvmCreatorWindow = window;
	}

	/**
	 * @return JVM creator window.
	 */
	public Stage getJvmCreatorWindow() {
		return jvmCreatorWindow;
	}

	/**
	 * @param window
	 * 		History window.
	 */
	public void setHistoryWindow(Stage window) {
		this.historyWindow = window;
	}

	/**
	 * @return History window.
	 */
	public Stage getHistoryWindow() {
		return historyWindow;
	}

	/**
	 * @return Information window.
	 */
	public Stage getInformationWindow() {
		return informationWindow;
	}

	/**
	 * @param window
	 * 		Information window.
	 */
	public void setInformationWindow(Stage window) {
		this.informationWindow = window;
	}

	/**
	 * @return Contact window.
	 */
	public Stage getContactWindow() {
		return contactWindow;
	}

	/**
	 * @param window
	 * 		Contact window.
	 */
	public void setContactWindow(Stage window) {
		this.contactWindow = window;
	}

	/**
	 * @return Plugin manager window.
	 */
	public Stage getPluginsWindow() {
		return pluginsWindow;
	}

	/**
	 * @param window
	 * 		Plugin manager window.
	 */
	public void setPluginsWindow(Stage window) {
		this.pluginsWindow = window;
	}
}
