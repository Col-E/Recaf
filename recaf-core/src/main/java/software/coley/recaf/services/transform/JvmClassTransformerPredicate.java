package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Predicate for preventing transformation of classes in {@link TransformationApplier}.
 *
 * @author Matt Coley
 */
public interface JvmClassTransformerPredicate {
	/**
	 * @param workspace
	 * 		Workspace containing the class.
	 * @param resource
	 * 		Resource containing the class.
	 * @param bundle
	 * 		Bundle containing the class.
	 * @param classInfo
	 * 		The class to transform.
	 *
	 * @return {@code true} to allow the class to be transformed.
	 * {@code false} to skip transforming the given class.
	 */
	boolean shouldTransform(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource,
	                        @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo classInfo);
}
