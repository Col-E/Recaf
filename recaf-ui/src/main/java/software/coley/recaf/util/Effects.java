package software.coley.recaf.util;

import javafx.scene.effect.BlurType;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;

/**
 * Various effect constants.
 *
 * @author Matt Coley
 */
public class Effects {
	/**
	 * Red border for controls with erroneous inputs.
	 */
	public static final InnerShadow ERROR_BORDER = new InnerShadow(BlurType.ONE_PASS_BOX, Color.RED, 4, 2, 0, 0);
}
