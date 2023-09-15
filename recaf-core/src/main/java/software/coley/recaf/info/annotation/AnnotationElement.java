package software.coley.recaf.info.annotation;

import jakarta.annotation.Nonnull;

/**
 * Outline of an annotation member.
 *
 * @author Matt Coley
 */
public interface AnnotationElement {
	/**
	 * @return Element name.
	 */
	@Nonnull
	String getElementName();

	/**
	 * @return Element value.
	 */
	@Nonnull
	Object getElementValue();
}
