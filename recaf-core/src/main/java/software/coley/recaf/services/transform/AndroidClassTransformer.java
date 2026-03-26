package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import me.darknet.dex.tree.definitions.ClassDefinition;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Outlines the base Android transformation contract.
 * <p>
 * <b>NOTE:</b> Internal transformers must be {@link Dependent} scoped so that they do not get proxied by CDI.
 *
 * @author Matt Coley
 */
public interface AndroidClassTransformer extends ClassTransformer {
	/**
	 * Used to do any workspace-scope setup actions before transformations occur.
	 *
	 * @param context
	 * 		Transformation context for access to other transformers and recording class changes.
	 * @param workspace
	 * 		Workspace containing classes to transform.
	 */
	default void setup(@Nonnull AndroidTransformerContext context, @Nonnull Workspace workspace) {}

	/**
	 * Implementations can {@link #dependencies() depend on other transformers} and access them
	 * via {@link AbstractTransformerContext#getTransformer(Class)}. This may be useful in cases where you want to have
	 * one transformer act as a shared data-storage between multiple transformers.
	 * <p>
	 * To record changes to the given {@code classInfo} you can:
	 * <ul>
	 *     <li>Record a {@link ClassDefinition} via {@link AndroidTransformerContext#setDefinition(AndroidClassInfo, ClassDefinition)}</li>
	 * </ul>
	 *
	 * @param context
	 * 		Transformation context for access to other transformers and recording class changes.
	 * @param workspace
	 * 		Workspace containing the class.
	 * @param resource
	 * 		Resource containing the class.
	 * @param bundle
	 * 		Bundle containing the class.
	 * @param initialClassState
	 * 		The initial state of the class to transform.
	 * 		Do not use this as the base of any transformation.
	 * 		Use {@link AndroidTransformerContext#getDefinition(AndroidClassInfo)}
	 * 		to look up the current transformed state of the class.
	 *
	 * @throws TransformationException
	 * 		When the class cannot be transformed for any reason.
	 */
	void transform(@Nonnull AndroidTransformerContext context, @Nonnull Workspace workspace,
	               @Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle,
	               @Nonnull AndroidClassInfo initialClassState) throws TransformationException;
}
