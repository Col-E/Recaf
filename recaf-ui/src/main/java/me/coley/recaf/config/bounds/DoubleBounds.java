package me.coley.recaf.config.bounds;

import me.coley.recaf.config.ConfigID;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Range definition for some {@link ConfigID} annotated field.
 *
 * @author Matt Coley
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DoubleBounds {
	/**
	 * @return Lower bound range, inclusive.
	 */
	double min();

	/**
	 * @return Upper bound range, inclusive.
	 */
	double max();
}
