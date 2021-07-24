package dev.xdak.recaf.plugin.java;

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

    String name();

    String version();

    String author() default "";

    String description() default "";
}
