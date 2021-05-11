package me.coley.recaf.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation applied to a field to create groups inside of config classes.
 * For example, in the UI config class there could be groupings for:
 * <ul>
 *     <li>Tree display options</li>
 *     <li>Code display options</li>
 *     <li>Theme/style options</li>
 *     <li>etc</li>
 * </ul>
 *
 * @author Matt Coley
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Group {
	/**
	 * A group is applied to some fields of a class containing {@link ConfigID} marked fields.
	 * The class container will have the major category grouping, and this will supply optional sub-groups
	 * for some fields.
	 * <br>
	 * This can allow similar item groups under the major category to be grouped together.
	 *
	 * @return Translation key suffix for the category, creating an effective sub-group.
	 */
	String value();
}
