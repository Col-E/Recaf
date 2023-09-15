package software.coley.recaf.info.annotation;

import jakarta.annotation.Nullable;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.TypeReference;

/**
 * Outline of type annotation data.
 *
 * @author Matt Coley
 */
public interface TypeAnnotationInfo extends AnnotationInfo {
	/**
	 * @return Constant denoting where the annotation is applied.
	 *
	 * @see TypeReference For return values.
	 */
	int getTypeRef();

	/**
	 * @return Path to a type argument. May be {@code null} if no path is required.
	 */
	@Nullable
	TypePath getTypePath();
}
