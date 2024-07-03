package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * An alternative to {@link ImageView} which allows rendering lower res images with less aggressive linear filtering.
 *
 * @author Matt Coley
 */
public class ImageCanvas extends Canvas {
	private Image image;
	private ColorAdjust colorAdjust;
	private double saturation;
	private double brightness;
	private double contrast;

	/**
	 * @param image
	 * 		Image to draw.
	 */
	public void setImage(@Nonnull Image image) {
		this.image = image;

		// By default, the canvas is 0 by 0.
		setWidth(image.getWidth());
		setHeight(image.getHeight());

		draw();
	}

	/**
	 * @param brightness
	 * 		New relative brightness adjustment.
	 */
	public void setBrightness(double brightness) {
		this.brightness = brightness;
		colorAdjust = new ColorAdjust(0, saturation, brightness, contrast);
		draw();
	}

	/**
	 * @param contrast
	 * 		New relative contrast adjustment.
	 */
	public void setContrast(double contrast) {
		this.contrast = contrast;
		colorAdjust = new ColorAdjust(0, saturation, brightness, contrast);
		draw();
	}

	private void draw() {
		// Draw the image with smoothing disabled.
		// With smoothing, it uses an aggressive linear filter which looks horrible on pixel-art/low-res images.
		// Without smoothing, it still uses a linear filter, but it is MUCH less aggressive.
		GraphicsContext gc = getGraphicsContext2D();
		gc.setEffect(null); // Clearing won't work if certain effects are applied, so reset it before clearing.
		gc.clearRect(0, 0, getWidth(), getHeight());
		if (image != null) {
			if (colorAdjust != null)
				gc.setEffect(colorAdjust);
			gc.setImageSmoothing(false);
			gc.drawImage(image, 0, 0);
		}
	}
}