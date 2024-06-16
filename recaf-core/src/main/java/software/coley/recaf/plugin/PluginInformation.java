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
	 * @return ID of the plugin.
	 */
	String id();

	/**
	 * @return Name of the plugin.
	 */
	String name();

	/**
	 * @return Version of the plugin.
	 */
	String version();

	/**
	 * @return Author of the plugin.
	 */
	String author() default "";

	/**
	 * @return Description of the plugin.
	 */
	String description() default "";

	/**
	 * @return Plugin dependencies (IDs of dependency plugins).
	 */
	String[] dependencies() default {};

	/**
	 * @return Plugin soft dependencies (IDs of dependency plugins).
	 */
	String[] softDependencies() default {};
}
