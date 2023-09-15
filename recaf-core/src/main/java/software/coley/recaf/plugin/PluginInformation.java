package software.coley.recaf.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Plugin annotation containing necessary information.
 *
 * @author xDark
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PluginInformation {
	/**
	 * @return name of the plugin.
	 */
	String name();

	/**
	 * @return version of the plugin.
	 */
	String version();

	/**
	 * @return author of the plugin.
	 */
	String author() default "";

	/**
	 * @return description of the plugin.
	 */
	String description() default "";
}