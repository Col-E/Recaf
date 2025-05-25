package software.coley.recaf.ui.window;

import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * Scene extension for adding additional style-sheets.
 *
 * @author Matt Coley
 */
public class RecafScene extends Scene {
	/**
	 * @param root
	 * 		Root node of the scene graph
	 */
	public RecafScene(Parent root) {
		super(root);
		addStyleSheets();
	}

	/**
	 * @param root
	 * 		Root node of the scene graph
	 * @param width
	 * 		Root node width.
	 * @param height
	 * 		Root node height.
	 */
	public RecafScene(Parent root, double width, double height) {
		super(root, width, height);
		addStyleSheets();
	}

	private void addStyleSheets() {
		getStylesheets().addAll("/style/docking.css", "/style/tweaks.css");
	}
}
