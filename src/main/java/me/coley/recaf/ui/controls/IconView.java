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
		super(new Image(resource(path)));
		fitHeightProperty().set(ICON_SIZE);
		fitWidthProperty().set(ICON_SIZE);
	}
}
