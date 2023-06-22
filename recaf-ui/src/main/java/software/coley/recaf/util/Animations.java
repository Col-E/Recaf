package software.coley.recaf.util;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Animation utilities.
 *
 * @author Matt Coley
 */
public class Animations {
	/**
	 * Play an animation to visual clarity <i>(Thin blue border)</i>.
	 *
	 * @param node
	 * 		Node to animate.
	 * @param millis
	 * 		Duration in milliseconds of fade.
	 */
	public static void animateNotice(Node node, long millis) {
		animate(node, millis, 100, 165, 255);
	}

	/**
	 * Play an animation that indicates success <i>(Thin green border)</i>.
	 *
	 * @param node
	 * 		Node to animate.
	 * @param millis
	 * 		Duration in milliseconds of fade.
	 */
	public static void animateSuccess(Node node, long millis) {
		animate(node, millis, 90, 255, 60);
	}

	/**
	 * Play an animation that indicates a warning <i>(Thin yellow border)</i>.
	 *
	 * @param node
	 * 		Node to animate.
	 * @param millis
	 * 		Duration in milliseconds of fade.
	 */
	public static void animateWarn(Node node, long millis) {
		animate(node, millis, 255, 255, 40);
	}

	/**
	 * Play an animation that indicates failure <i>(Thin red border)</i>.
	 *
	 * @param node
	 * 		Node to animate.
	 * @param millis
	 * 		Duration in milliseconds of fade.
	 */
	public static void animateFailure(Node node, long millis) {
		animate(node, millis, 255, 60, 40);
	}

	private static void animate(Node node, long millis, int r, int g, int b) {
		DoubleProperty dblProp = new SimpleDoubleProperty(1);
		dblProp.addListener((ob, o, n) -> {
			InnerShadow innerShadow = new InnerShadow();
			innerShadow.setBlurType(BlurType.ONE_PASS_BOX);
			innerShadow.setChoke(1);
			innerShadow.setRadius(5);
			innerShadow.setColor(Color.rgb(r, g, b, n.doubleValue()));
			node.setEffect(innerShadow);
		});
		Timeline timeline = new Timeline();
		KeyValue kv = new KeyValue(dblProp, 0);
		KeyFrame kf = new KeyFrame(Duration.millis(millis), kv);
		timeline.getKeyFrames().add(kf);
		timeline.play();
	}
}