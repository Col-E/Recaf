package software.coley.recaf.info.annotation;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;

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
	 * @return Element value. Can be a primitive, {@link String}, a {@link AnnotationInfo},
	 * a {@link AnnotationEnumReference}, a {@link Type}, or a {@link List} of any of the prior values.
	 */
	@Nonnull
	Object getElementValue();
}
