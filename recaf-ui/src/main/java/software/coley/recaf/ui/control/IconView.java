package software.coley.recaf.ui.control;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import software.coley.recaf.util.Icons;

/**
 * {@link ImageView} extension for icons. It is recommended to use use {@link Icons}
 * to fetch {@link Image} instances, and generally creating instances of this class.
 *
 * @author Matt Coley
 * @see Icons
 */
public class IconView extends ImageView {
	/**
	 * Default icon size, 16x16.
	 */
	public static final int DEFAULT_ICON_SIZE = 16;

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