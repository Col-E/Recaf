package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.ClassNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collections;
import java.util.Set;

/**
 * Outlines the base JVM transformation contract.
 *
 * @author Matt Coley
 */
public interface JvmClassTransformer {
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
	 * @param classInfo
	 * 		The class to transform.
	 *
	 * @throws TransformationException
	 * 		When the class cannot be transformed for any reason.
	 */
	void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	               @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	               @Nonnull JvmClassInfo classInfo) throws TransformationException;

	/**
	 * @return Name of the transformer.
	 */
	@Nonnull
	String name();

	/**
	 * @return Set of transformer classes that must run before this one.
	 */
	@Nonnull
	default Set<Class<? extends JvmClassTransformer>> dependencies() {
		return Collections.emptySet();
	}
}
