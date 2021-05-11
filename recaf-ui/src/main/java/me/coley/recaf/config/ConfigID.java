package me.coley.recaf.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation applied to a field to indicate it should be serialized/deserialized and shown in the config UI.
 * Supplies UI text for the field via the translation key given in {@link #value()}.
 *
 * @author Matt Coley
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigID {
	/**
	 * @return Translation key for fetching name and descriptions of the config value.
	 */
	String value();

	/**
	 * Hidden values can be used for more backend/testing settings.
	 *
	 * @return {@code true} to hide from UI.
	 */
	boolean hidden() default false;

	/**
	 * Allow the {@link #value()} to be marked as translatable.
	 * Normally all items should be, but in some cases <i>(plugins)</i> it may not be
	 * immediately available as an option.
	 *
	 * @return {@code true} when the {@link #value()} indicates a translation key.
	 * {@code false} when it is literal text.
	 */
	boolean translatable() default true;
}
