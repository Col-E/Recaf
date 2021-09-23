package me.coley.recaf.ui.control.config;

import javafx.scene.control.Slider;
import me.coley.recaf.config.*;
import me.coley.recaf.util.ReflectUtil;

import java.lang.reflect.Field;

/**
 * Component for {@code int/float/double/long} config values that are bound to a range.
 *
 * @author Matt Coley
 */
public class ConfigRanged extends Slider implements Unlabeled {
	/**
	 * @param instance
	 * 		Config container.
	 * @param field
	 * 		Config field.
	 */
	public ConfigRanged(ConfigContainer instance, Field field) {
		double min = getMin(field);
		double max = getMax(field);
		int majorTick = (int) ((max - min) / 10);
		setMin(min);
		setMax(max);
		setMajorTickUnit(majorTick);
		setShowTickMarks(true);
		setShowTickLabels(true);
		setSnapToTicks(true);
		double value = ((Number) ReflectUtil.quietGet(instance, field)).doubleValue();
		setValue(value);
		valueProperty().addListener((observable, old, current) ->
				ReflectUtil.quietSet(instance, field, adapt(field, current)));
	}

	/**
	 * @param field
	 * 		Field to check for a bounds annotation.
	 *
	 * @return {@link true} when a bounds annotation is found.
	 */
	public static boolean hasBounds(Field field) {
		return field.getAnnotation(IntBounds.class) != null
				|| field.getAnnotation(DoubleBounds.class) != null
				|| field.getAnnotation(LongBounds.class) != null
				|| field.getAnnotation(FloatBounds.class) != null;
	}

	private static Object adapt(Field field, Number current) {
		Class<?> type = field.getType();
		if (int.class.equals(type) || Integer.class.equals(type))
			return current.intValue();
		else if (double.class.equals(type) || Double.class.equals(type))
			return current.doubleValue();
		else if (long.class.equals(type) || Long.class.equals(type))
			return current.longValue();
		else if (float.class.equals(type) || Float.class.equals(type))
			return current.floatValue();
		throw new IllegalStateException("Unsupported primitive: " + type);
	}

	private static double getMin(Field field) {
		IntBounds intBounds = field.getAnnotation(IntBounds.class);
		if (intBounds != null)
			return intBounds.min();
		DoubleBounds doubleBounds = field.getAnnotation(DoubleBounds.class);
		if (doubleBounds != null)
			return doubleBounds.min();
		LongBounds longBounds = field.getAnnotation(LongBounds.class);
		if (longBounds != null)
			return longBounds.min();
		FloatBounds floatBounds = field.getAnnotation(FloatBounds.class);
		if (floatBounds != null)
			return floatBounds.min();
		throw new IllegalStateException("No bounds found for field: " + field.getName());
	}

	private static double getMax(Field field) {
		IntBounds intBounds = field.getAnnotation(IntBounds.class);
		if (intBounds != null)
			return intBounds.max();
		DoubleBounds doubleBounds = field.getAnnotation(DoubleBounds.class);
		if (doubleBounds != null)
			return doubleBounds.max();
		LongBounds longBounds = field.getAnnotation(LongBounds.class);
		if (longBounds != null)
			return longBounds.max();
		FloatBounds floatBounds = field.getAnnotation(FloatBounds.class);
		if (floatBounds != null)
			return floatBounds.max();
		throw new IllegalStateException("No bounds found for field: " + field.getName());
	}
}
