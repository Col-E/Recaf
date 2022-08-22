package me.coley.recaf.config;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for ignoring fields in config classes.
 * @author Amejonah
 */
@Target(java.lang.annotation.ElementType.FIELD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Ignore {}
