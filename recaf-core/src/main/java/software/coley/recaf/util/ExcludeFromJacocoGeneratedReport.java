package software.coley.recaf.util;

import jakarta.annotation.Nonnull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JaCoCo excludes coverage metrics from classes and methods annotated with names including {@code "Generated"}.
 * <p>
 * <h1>USE THIS CLASS SPARINGLY</h1>
 * <b>Only annotate things with this if they are POJO's!</b>
 * Some classes like data models and config objects contribute to coverage metrics with things like getter/setters
 * that do not realistically need to be covered.
 *
 * @author Matt Coley
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ExcludeFromJacocoGeneratedReport {
	/**
	 * @return Reason why we exclude the annotated element.
	 */
	@Nonnull
	String justification();
}