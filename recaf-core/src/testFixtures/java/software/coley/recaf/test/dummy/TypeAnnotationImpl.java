package software.coley.recaf.test.dummy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Dummy annotation to test type annotations with.
 */
@SuppressWarnings("all")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.TYPE_PARAMETER, ElementType.TYPE_USE, ElementType.PARAMETER})
public @interface TypeAnnotationImpl {
	String value();
}
