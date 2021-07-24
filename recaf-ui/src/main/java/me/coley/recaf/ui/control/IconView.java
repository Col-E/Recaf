package me.coley.recaf.ui.control;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import me.coley.recaf.util.ResourceUtil;

/**
 * ImageView extension for icons.
 *
 * @author Matt Coley
 */
public class IconView extends ImageView {
	private static final int DEFAULT_ICON_SIZE = 16;

	/**
	 * @param path
	 * 		Path to resource.
	 */
	public IconView(String path) {
		this(new Image(ResourceUtil.resource(path)), DEFAULT_ICON_SIZE);
	}

	/**
	 * @param path
	 * 		Path to resource.
	 * @param size
	 * 		Image width/height.
	 */
	public IconView(String path, int size) {
		this(new Image(ResourceUtil.resource(path)), size);
	}

	/**
	 * @param image
	 * 		Image resource.
	 */
	public IconView(Image image) {
		this(image, DEFAULT_ICON_SIZE);
	}

	/**
	 * @param image
	 * 		Image resource.
	 * @param size
	 * 		Image width/height.
	 */
	public IconView(Image image, int size) {
		super(image);
		fitHeightProperty().set(size);
		fitWidthProperty().set(size);
		// Setting to false seems to make smaller images scale properly (like 10x10)
		setPreserveRatio(false);
	}
}