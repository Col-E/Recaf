package me.coley.recaf.config.bounds;

import me.coley.recaf.config.ConfigID;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lower bound definition for some {@link ConfigID} annotated field.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LongLowerBound {
	/**
	 * @return Lower bound range, inclusive.
	 */
	long value();
}
