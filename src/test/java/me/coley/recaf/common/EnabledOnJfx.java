package me.coley.recaf.common;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;


/**
 * Annotation to indicate test class/test methods should only be run if JavaFx can be initialized.
 *
 * @author Matt
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(JavaFxCondition.class)
public @interface EnabledOnJfx {}
