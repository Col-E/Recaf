package me.coley.recaf.ui.window;

import javafx.scene.Parent;
import javafx.scene.Scene;

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
	}

	@Override
	protected Scene createScene() {
		if (width == 0 || height == 0)
			return new Scene(content);
		else
			return new Scene(content, width, height);
	}
}
