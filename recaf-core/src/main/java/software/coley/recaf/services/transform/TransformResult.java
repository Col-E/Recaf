package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Map;

/**
 * Intermediate holder of transformations of workspace classes. You can inspect the state of the transformed classes
 * before you apply the changes to the associated {@link Workspace}.
 *
 * @author Matt Coley
 */
public interface TransformResult {
	/**
	 * Puts the transformed classes into the associated workspace.
	 */
	void apply();

	/**
	 * @return Map of classes, to their maps of transformer-associated exceptions.
	 * Empty if transformation was a complete success <i>(no failures)</i>.
	 */
	@Nonnull
	Map<ClassPathNode, Map<Class<? extends JvmClassTransformer>, Throwable>> getJvmTransformerFailures();

	/**
	 * @return Map of class paths to the original classes, to the resulting transformed class models.
	 */
	@Nonnull
	Map<ClassPathNode, JvmClassInfo> getJvmTransformedClasses();
}
