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
		// All of our defined stylesheets are added to all scenes. This is actually good for performance overall.
		// In some cases when a 'Node' updates and recomputes what styles it has, having local stylesheets results
		// in a lot of extra work. The library we use for our virtualized components falls victim to this making
		// the components very sluggish. To fix the issue, everything gets registered at the scene level.
		//
		// The 'recaf.css' stylesheet is the application baseline sheet and does not need to be referenced here.
		getStylesheets().addAll(
				"/style/code-editor.css",
				"/style/docking.css",
				"/style/hex.css",
				"/style/tweaks.css"
		);
	}
}
