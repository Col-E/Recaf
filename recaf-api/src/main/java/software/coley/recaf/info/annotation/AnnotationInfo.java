package software.coley.recaf.info.annotation;


import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Outline of annotation data.
 *
 * @author Matt Coley
 */
public interface AnnotationInfo {
	/**
	 * @return {@code true} if the annotation is visible at runtime.
	 */
	boolean isVisible();

	/**
	 * @return Annotation descriptor.
	 */
	@Nonnull
	String getDescriptor();

	/**
	 * @return Annotation elements.
	 */
	@Nonnull
	Map<String, AnnotationElement> getElements();
}
