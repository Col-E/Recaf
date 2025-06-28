package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import javafx.scene.paint.Color;

/**
 * Utils for working with colors.
 *
 * @author Matt Coley
 */
public class Colors {
	/**
	 * From: <a href="https://github.com/sfPlayer1/Matcher/blob/d7fc31b318ea86ac4422237ad204622160d08db8/src/matcher/gui/MatchPaneSrc.java#L315">
	 * Matcher</a>
	 *
	 * @param colorA
	 * 		First color.
	 * @param colorB
	 * 		Second color.
	 * @param step
	 * 		Some ratio of conversion, where {@code 0.0} is {@code colorA} and {@code 1.0} is {@code colorB}.
	 *
	 * @return Interpolated color from HSB.
	 */
	@Nonnull
	public static Color interpolateHsb(@Nonnull Color colorA, @Nonnull Color colorB, float step) {
		float hueA = (float) colorA.getHue();
		float hueB = (float) colorB.getHue();

		// Hue interpolation
		float hue;
		float hueDiff = hueB - hueA;

		if (hueA > hueB) {
			float tempHue = hueB;
			hueB = hueA;
			hueA = tempHue;

			Color tempColor = colorB;
			colorB = colorA;
			colorA = tempColor;

			hueDiff = -hueDiff;
			step = 1 - step;
		}

		if (hueDiff <= 180) {
			hue = hueA + step * hueDiff;
		} else {
			hueA = hueA + 360;
			hue = (hueA + step * (hueB - hueA)) % 360;
		}

		// Interpolate the rest
		return Color.hsb(
				hue,
				colorA.getSaturation() + step * (colorB.getSaturation() - colorA.getSaturation()),
				colorA.getBrightness() + step * (colorB.getBrightness() - colorA.getBrightness()));
	}

	/**
	 * @param color
	 * 		Color to convert.
	 *
	 * @return ARGB of color.
	 */
	public static int argb(@Nonnull Color color) {
		int a = (int) (color.getOpacity() * 255);
		int r = (int) (color.getRed() * 255);
		int g = (int) (color.getGreen() * 255);
		int b = (int) (color.getBlue() * 255);
		return a << 24 | r << 16 | g << 8 | b;
	}
}
