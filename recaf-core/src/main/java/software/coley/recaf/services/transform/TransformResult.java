package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.ClassPathNode;

import java.util.Map;

/**
 * Intermediate holder of transformations of workspace classes.
 *
 * @author Matt Coley
 */
public interface TransformResult<CT extends ClassTransformer, CI extends ClassInfo> {
	/**
	 * Puts the transformed classes into the associated workspace.
	 * You can inspect the state of transformed classes before this via {@link #getTransformedClasses()}.
	 */
	void apply();

	/**
	 * @return Map of classes, to their maps of transformer-associated exceptions.
	 * Empty if transformation was a complete success <i>(no failures)</i>.
	 */
	@Nonnull
	Map<ClassPathNode, Map<Class<? extends CT>, Throwable>> getTransformerFailures();

	/**
	 * @return Map of class paths to the original classes, to the resulting transformed class models.
	 */
	@Nonnull
	Map<ClassPathNode, CI> getTransformedClasses();
}
