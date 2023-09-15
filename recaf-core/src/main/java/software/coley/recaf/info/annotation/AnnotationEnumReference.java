package software.coley.recaf.info.annotation;

import jakarta.annotation.Nonnull;

/**
 * Outline of an enum reference for {@link AnnotationElement#getElementValue()}.
 *
 * @author Matt Coley
 */
public interface AnnotationEnumReference {
	/**
	 * @return Descriptor of enum value.
	 */
	@Nonnull
	String getDescriptor();

	/**
	 * @return Enum value name.
	 */
	@Nonnull
	String getValue();
}
