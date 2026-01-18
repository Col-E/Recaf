package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Outline of transformation feedback capabilities. Allows for:
 * <ul>
 *     <li>In-progress transformation cancellation</li>
 *     <li>Filter classes transformed</li>
 *     <li>Notifying transformation progress</li>
 * </ul>
 *
 * @author Matt Coley
 * @see CancellableTransformationFeedback Basic cancellable implementation.
 */
public interface TransformationFeedback {
	/**
	 * Default implementation that runs transformations to completion.
	 */
	TransformationFeedback DEFAULT = new TransformationFeedback() {
	};

	/**
	 * @return {@code true} to request {@link TransformationApplier} stops handling input to end the transformation early.
	 * {@code false} to continue the transformation.
	 */
	default boolean hasRequestedCancellation() {
		return false;
	}

	/**
	 * Called before a class is transformed.
	 *
	 * @param workspace
	 * 		Workspace containing the class.
	 * @param resource
	 * 		Resource containing the class.
	 * @param bundle
	 * 		Bundle containing the class.
	 * @param classInfo
	 * 		The class to transform.
	 * @param transformer
	 * 		Transformer to apply.
	 * @param pass
	 * 		The current transformation pass.
	 *
	 * @return {@code true} to allow the class to be transformed.
	 * {@code false} to skip transforming the given class.
	 */
	default boolean shouldTransform(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource,
	                                @Nonnull ClassBundle<?> bundle, @Nonnull ClassInfo classInfo,
	                                @Nonnull ClassTransformer transformer, int pass) {
		return true;
	}

	/**
	 * Called when a class has been successfully transformed.
	 *
	 * @param workspace
	 * 		Workspace containing the class.
	 * @param resource
	 * 		Resource containing the class.
	 * @param bundle
	 * 		Bundle containing the class.
	 * @param classInfo
	 * 		The original class transformed.
	 * @param transformer
	 * 		Transformer applied.
	 * @param pass
	 * 		The current transformation pass.
	 */
	default void onTransformed(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource,
	                           @Nonnull ClassBundle<?> bundle, @Nonnull ClassInfo classInfo,
	                           @Nonnull ClassTransformer transformer, int pass) {}

	/**
	 * Called when a class passed transformation, but no work was done.
	 *
	 * @param workspace
	 * 		Workspace containing the class.
	 * @param resource
	 * 		Resource containing the class.
	 * @param bundle
	 * 		Bundle containing the class.
	 * @param classInfo
	 * 		The original class transformed.
	 * @param transformer
	 * 		Transformer applied.
	 * @param pass
	 * 		The current transformation pass.
	 */
	default void onTransformedWithoutWork(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource,
	                                      @Nonnull ClassBundle<?> bundle, @Nonnull ClassInfo classInfo,
	                                      @Nonnull ClassTransformer transformer, int pass) {}

	/**
	 * Called when a transformation on a class fails.
	 *
	 * @param workspace
	 * 		Workspace containing the class.
	 * @param resource
	 * 		Resource containing the class.
	 * @param bundle
	 * 		Bundle containing the class.
	 * @param classInfo
	 * 		The original class transformed.
	 * @param transformer
	 * 		Transformer applied.
	 * @param pass
	 * 		The current transformation pass.
	 * @param error
	 * 		The exception thrown during transformation.
	 */
	default void onTransformFailure(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource,
	                                @Nonnull ClassBundle<?> bundle, @Nonnull ClassInfo classInfo,
	                                @Nonnull ClassTransformer transformer, int pass, @Nonnull Throwable error) {}

	/**
	 * Called when the transformation completes.
	 */
	default void onCompletion() {}

}
