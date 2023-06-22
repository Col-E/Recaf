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
@Target({ElementType.TYPE_PARAMETER})
public @interface TypeAnnotationImpl {
	String value();
}
