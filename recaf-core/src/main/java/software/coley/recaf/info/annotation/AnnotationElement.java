package software.coley.recaf.info.annotation;

import jakarta.annotation.Nonnull;

import java.util.List;

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
	 * @return Element value. Can be a primitive, {@link String}, a {@link AnnotationElement}, or a {@link List} of any of the prior values.
	 */
	@Nonnull
	Object getElementValue();
}
