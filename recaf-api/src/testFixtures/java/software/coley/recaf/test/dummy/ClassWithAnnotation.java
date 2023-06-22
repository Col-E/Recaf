package software.coley.recaf.test.dummy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Test for recursive annotation usage.
 */
@AnnotationImpl(
		value = "Hello",
		policy = @Retention(
				value = RetentionPolicy.CLASS
		)
)
@SuppressWarnings("all")
public class ClassWithAnnotation<@TypeAnnotationImpl("arg") S> {
}
