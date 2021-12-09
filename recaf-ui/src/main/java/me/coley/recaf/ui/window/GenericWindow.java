package me.coley.recaf.ui.window;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.WindowEvent;
import me.coley.recaf.ui.behavior.OnCloseListener;
import me.coley.recaf.ui.behavior.OnShownListener;

/**
 * A generic window that takes a {@link Parent} for its content.
 *
 * @author Matt Coley
 */
public class GenericWindow extends WindowBase {
	private final Parent content;
	private final int width;
	private final int height;

	/**
	 * @param content
	 * 		Window content.
	 */
	public GenericWindow(Parent content) {
		this(content, 0, 0);
	}

	/**
	 * @param content
	 * 		Window content.
	 * @param width
	 * 		Content width.
	 * @param height
	 * 		Content height.
	 */
	public GenericWindow(Parent content, int width, int height) {
		this.content = content;
		this.width = width;
		this.height = height;
		init();
		// Add listeners
		if (content instanceof OnShownListener) {
			OnShownListener listener = (OnShownListener) content;
			setOnShown(e -> {
				listener.onShown(e);
				setOnShown(null);
			});
		}
		if (content instanceof OnCloseListener) {
			OnCloseListener listener = (OnCloseListener) content;
			setOnCloseRequest(e -> {
				if (getScene().getRoot() == content)
					listener.onClose(e);
			});
		}
	}

	@Override
	protected Scene createScene() {
		if (width == 0 || height == 0)
			return new Scene(content);
		else
			return new Scene(content, width, height);
	}
}
