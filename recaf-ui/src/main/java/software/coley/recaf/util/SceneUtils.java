package software.coley.recaf.util;

import jakarta.annotation.Nullable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import software.coley.recaf.ui.docking.DockingRegion;
import software.coley.recaf.ui.docking.DockingTab;

/**
 * Scene utilities.
 *
 * @author Matt Coley
 */
public class SceneUtils {
	private SceneUtils() {}

	/**
	 * @param node
	 * 		Node within a target {@link Scene} to bring to front/focus.
	 */
	public static void focus(@Nullable Node node) {
		while (node != null) {
			// Get the parent of the node, skip the intermediate 'content area' from tab-pane default skin.
			Parent parent = node.getParent();
			if (parent != null && parent.getStyleClass().contains("tab-content-area"))
				parent = parent.getParent();

			// If the tab content is the node, select it and return.
			if (parent instanceof DockingRegion tabParent) {
				Scene scene = parent.getScene();
				for (DockingTab tab : tabParent.getDockTabs())
					if (tab.getContent() == node) {
						tab.select();
						SceneUtils.focus(scene);
						return;
					}
			}

			// Next parent.
			node = parent;
		}
	}

	/**
	 * @param scene
	 * 		Scene to bring to front/focus.
	 */
	public static void focus(@Nullable Scene scene) {
		if (scene == null)
			return;

		Window window = scene.getWindow();
		if (window instanceof Stage stage) {
			// If minified, unminify it.
			stage.setIconified(false);
			stage.show();

			// The method 'stage.toFront()' does not work as you'd expect so this hack is how we
			// force the window to the front.
			stage.setAlwaysOnTop(true);
			stage.setAlwaysOnTop(false);
		}
		window.requestFocus();
	}

}
