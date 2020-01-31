package me.coley.recaf.ui.controls;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import static me.coley.recaf.util.ClasspathUtil.resource;

/**
 * ImageView extension for icons.
 */
public class IconView extends ImageView {
	private static final int ICON_SIZE = 16;

	/**
	 * @param path
	 * 		Path to resource.
	 */
	public IconView(String path) {
		this(new Image(resource(path)));
	}

	/**
	 * @param image
	 * 		Image resource.
	 */
	public IconView(Image image) {
		super(image);
		fitHeightProperty().set(ICON_SIZE);
		fitWidthProperty().set(ICON_SIZE);
	}
}
