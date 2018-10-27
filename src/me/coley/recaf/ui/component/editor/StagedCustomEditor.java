package me.coley.recaf.ui.component.editor;

import org.controlsfx.control.PropertySheet.Item;

import javafx.scene.Parent;
import javafx.stage.Stage;
import me.coley.recaf.ui.component.ReflectivePropertySheet.CustomEditor;
import me.coley.recaf.util.JavaFX;
import me.coley.recaf.util.Lang;

/**
 * CustomEditor with for displaying external stages, without duplication.
 * 
 * @author Matt
 *
 * @param <T>
 */
public abstract class StagedCustomEditor<T> extends CustomEditor<T> {
	/**
	 * Cached stage, allows the same instance to be saved after <>"closing"</> the window.
	 */
	protected Stage stage;

	public StagedCustomEditor(Item item) {
		super(item);
	}

	/**
	 * Bring window to front is stage exists.
	 * 
	 * @return
	 */
	protected boolean staged() {
		// Don't make duplicate windows if not needed
		if (stage != null) {
			stage.toFront();
			if (!stage.isShowing()) {
				stage.show();
			}
			return true;
		}
		return false;
	}

	/**
	 * Setup stage.
	 * 
	 * @param key
	 *            Translation key for title.
	 * @param node
	 *            Content.
	 * @param width
	 * @param height
	 */
	protected void setStage(String key, Parent node, int width, int height) {
		stage = JavaFX.stage(JavaFX.scene(node, width, height), Lang.get(key), true);
		stage.setOnCloseRequest(e -> stage = null);
		stage.show();
	}
}