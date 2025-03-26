package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import javafx.animation.FadeTransition;
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
		InnerShadow innerShadow = new InnerShadow();
		innerShadow.setBlurType(BlurType.ONE_PASS_BOX);
		innerShadow.setChoke(1);
		innerShadow.setRadius(5);
		DoubleProperty dblProp = new SimpleDoubleProperty(1);
		dblProp.addListener((ob, o, n) -> {
			double opacity = n.doubleValue();
			if (opacity > 0.1) {
				innerShadow.setColor(Color.rgb(r, g, b, opacity));
			} else {
				node.setEffect(null);
			}
		});
		Timeline timeline = new Timeline(15 /* Reduced framerate for less scene updates */);
		KeyValue kv = new KeyValue(dblProp, 0);
		KeyFrame kf = new KeyFrame(Duration.millis(millis), kv);
		node.setEffect(innerShadow);
		timeline.getKeyFrames().add(kf);
		timeline.play();
	}

	/**
	 * Registers mouse-enter/exit events which transition the opacity of the given node.
	 *
	 * @param node
	 * 		Not to install show-on-hover.
	 */
	public static void setupShowOnHover(@Nonnull Node node) {
		double hiddenOpacity = 0.1;
		DoubleProperty opacity = node.opacityProperty();
		opacity.set(hiddenOpacity);

		FadeTransition show = new FadeTransition(Duration.millis(250), node);
		show.setToValue(1.0);

		FadeTransition hide = new FadeTransition(Duration.millis(250), node);
		hide.setToValue(hiddenOpacity);

		node.setOnMouseEntered(e -> {
			show.setFromValue(opacity.doubleValue());
			show.play();
		});
		node.setOnMouseExited(e -> {
			hide.setFromValue(opacity.doubleValue());
			hide.play();
		});
	}
}