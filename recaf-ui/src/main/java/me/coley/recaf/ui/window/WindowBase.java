package me.coley.recaf.ui.window;

import com.panemu.tiwulfx.control.dock.TabStageAccessor;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.KeybindConfig;
import me.coley.recaf.ui.behavior.WindowCloseListener;
import me.coley.recaf.ui.behavior.WindowShownListener;
import me.coley.recaf.ui.prompt.QuickNavPrompt;
import me.coley.recaf.ui.util.Icons;

import java.util.Arrays;
import java.util.List;

/**
 * Base window attributes.
 *
 * @author Matt Coley
 */
public abstract class WindowBase extends Stage implements TabStageAccessor {
	/**
	 * Create the scene and add the base stylesheets.
	 */
	protected void init() {
		Scene scene = createScene();
		setScene(scene);
		installListeners(this, scene.getRoot());
		installGlobalBinds(this);
		installLogo(this);
		installStyle(scene.getStylesheets());
	}

	/**
	 * Handle window-level key events.
	 *
	 * @param stage
	 * 		Stage to register keybinds on.
	 * @param event
	 * 		Key event.
	 */
	private static void onKeyPressed(Stage stage, KeyEvent event) {
		KeybindConfig binds = Configs.keybinds();
		if (binds.isEditingBind())
			return;
		if (binds.fullscreen.match(event)) {
			stage.setFullScreen(!stage.isFullScreen());
		}
		if (binds.quickNav.match(event)) {
			QuickNavPrompt.open();
		}
	}

	/**
	 * @return Stage scene with prepared content.
	 */
	protected abstract Scene createScene();

	@Override
	public Stage getStage() {
		return this;
	}

	/**
	 * @param stage
	 * 		Stage to add logo to.
	 */
	public static void installLogo(Stage stage) {
		stage.getIcons().add(new Image(Icons.LOGO));
	}

	/**
	 * @param stage
	 * 		Stage to add global keybinds to.
	 */
	public static void installGlobalBinds(Stage stage) {
		Scene scene = stage.getScene();
		scene.setOnKeyPressed(e -> onKeyPressed(stage, e));
	}

	/**
	 * @param stage
	 * 		Stage to add support for window-level listeners for.
	 * @param root
	 * 		Root node of stage.
	 */
	public static void installListeners(Stage stage, Parent root) {
		if (root instanceof WindowCloseListener) {
			EventHandler<WindowEvent> oldHandler = stage.getOnCloseRequest();
			stage.setOnCloseRequest(e -> {
				if (root.getScene().getWindow() != stage)
					return;
				((WindowCloseListener) root).onClose(e);
				if (oldHandler != null) {
					oldHandler.handle(e);
				}
			});
		}
		if (root instanceof WindowShownListener) {
			EventHandler<WindowEvent> oldHandler = stage.getOnShown();
			stage.setOnShown(e -> {
				if (root.getScene().getWindow() != stage)
					return;
				((WindowShownListener) root).onShown(e);
				if (oldHandler != null) {
					oldHandler.handle(e);
				}
			});
		}
		for (Node child : root.getChildrenUnmodifiable()) {
			if (child instanceof Parent) {
				installListeners(stage, (Parent) child);
			}
		}
	}

	/**
	 * @param stylesheets
	 * 		Stylesheet list to update.
	 */
	public static void installStyle(List<String> stylesheets) {
		stylesheets.addAll(Arrays.asList("style/base.css",
				"style/button.css",
				"style/code.css",
				"style/cursor.css",
				"style/dialog.css",
				"style/diff.css",
				"style/hex.css",
				"style/hierarchy.css",
				"style/graph.css",
				"style/hyperlink.css",
				"style/list.css",
				"style/log.css",
				"style/markdown.css",
				"style/menu.css",
				"style/navbar.css",
				"style/progress.css",
				"style/scroll.css",
				"style/table.css",
				"style/tabs.css",
				"style/text.css",
				"style/tooltip.css",
				"style/tree.css",
				"style/tree-transparent.css",
				"style/split.css")
		);
	}
}
