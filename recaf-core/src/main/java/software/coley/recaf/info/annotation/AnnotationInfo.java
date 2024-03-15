package software.coley.recaf.info.annotation;


import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.TypePath;

import java.util.Map;

/**
 * Outline of annotation data.
 *
 * @author Matt Coley
 */
public interface AnnotationInfo {
	/**
	 * @param typeRef
	 * 		Constant denoting where the annotation is applied.
	 * @param typePath
	 * 		Path to a type argument.
	 *
	 * @return Type annotation from this annotation.
	 */
	@Nonnull
	TypeAnnotationInfo withTypeInfo(int typeRef, @Nullable TypePath typePath);

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
