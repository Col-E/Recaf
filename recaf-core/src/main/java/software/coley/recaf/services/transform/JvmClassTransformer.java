package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.tree.ClassNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Outlines the base JVM transformation contract.
 * <p/>
 * <b>NOTE:</b> Internal transformers must be {@link Dependent} scoped so that they do not get proxied by CDI.
 * See {@link JvmTransformerContext#getJvmTransformer(Class)}.
 *
 * @author Matt Coley
 */
public interface JvmClassTransformer extends ClassTransformer {
	/**
	 * Used to do any workspace-scope setup actions before transformations occur.
	 *
	 * @param context
	 * 		Transformation context for access to other transformers and recording class changes.
	 * @param workspace
	 * 		Workspace containing classes to transform.
	 */
	default void setup(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace) {}

	/**
	 * Implementations can {@link #dependencies() depend on other transformers} and access them
	 * via {@link JvmTransformerContext#getJvmTransformer(Class)}. This may be useful in cases where you want to have
	 * one transformer act as a shared data-storage between multiple transformers.
	 * <p>
	 * To record changes to the given {@code classInfo} you can:
	 * <ul>
	 *     <li>Record a {@link ClassNode} via {@link JvmTransformerContext#setNode(JvmClassBundle, JvmClassInfo, ClassNode)}</li>
	 *     <li>Record bytecode via {@link JvmTransformerContext#setBytecode(JvmClassBundle, JvmClassInfo, byte[])}</li>
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
	 * 		Use {@link JvmTransformerContext#getNode(JvmClassBundle, JvmClassInfo)}
	 * 		or {@link JvmTransformerContext#getBytecode(JvmClassBundle, JvmClassInfo)}
	 * 		to look up the current transformed state of the class.
	 *
	 * @throws TransformationException
	 * 		When the class cannot be transformed for any reason.
	 */
	void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	               @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	               @Nonnull JvmClassInfo initialClassState) throws TransformationException;
}
