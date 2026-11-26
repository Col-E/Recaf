package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import software.coley.recaf.behavior.PrioritySortable;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Resolves obfuscator-specific {@code invokedynamic} calls into direct JVM member handles.
 *
 * @author Matt Coley
 * @see InvokeDynamicInliningTransformer
 * @see InvokeDynamicResolverManager
 */
public interface InvokeDynamicResolver extends PrioritySortable {
	/**
	 * Performs workspace-scoped resolver setup before class transformations begin.
	 *
	 * @param context
	 * 		Transformation context for the current run.
	 * @param workspace
	 * 		Workspace containing classes to transform.
	 *
	 * @throws TransformationException
	 * 		When resolver setup cannot complete.
	 */
	default void setup(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace) throws TransformationException {
		// no-op
	}

	/**
	 * Attempts to resolve one reachable dynamic call site.
	 *
	 * @param context
	 * 		Transformation context for the current run.
	 * @param workspace
	 * 		Workspace containing the class being transformed.
	 * @param classNode
	 * 		Current class node containing the call site.
	 * @param method
	 * 		Method containing the call site.
	 * @param instruction
	 * 		Dynamic call site to inspect.
	 * @param frame
	 * 		Frame immediately before the call site.
	 *
	 * @return Direct target and number of trailing call-site arguments to remove, or {@code null} when this
	 * resolver does not recognize the site or cannot provide a safe replacement.
	 */
	@Nullable
	ResolvedInvokeDynamic resolve(@Nonnull JvmTransformerContext context,
	                              @Nonnull Workspace workspace,
	                              @Nonnull ClassNode classNode,
	                              @Nonnull MethodNode method,
	                              @Nonnull InvokeDynamicInsnNode instruction,
	                              @Nonnull Frame<ReValue> frame);

	/**
	 * Attempts to resolve a dynamic call site when the primary analyzer did not provide a frame.
	 * <p>
	 * Resolvers should override this only when they can recover all required state directly from the instruction and
	 * surrounding bytecode without needing stack/locals state. This is a fallback for when the analyzer finds
	 * a call site that is deemed unreachable <i>(Which can be from genuine dead code, or intermediate transformations
	 * making the path to the site appear dead)</i>.
	 *
	 * @param context
	 * 		Transformation context for the current run.
	 * @param workspace
	 * 		Workspace containing the class being transformed.
	 * @param classNode
	 * 		Current class node containing the call site.
	 * @param method
	 * 		Method containing the call site.
	 * @param instruction
	 * 		Dynamic call site to inspect.
	 *
	 * @return Direct target and number of trailing call-site arguments to remove, or {@code null} when this resolver
	 * cannot recover the site without a frame.
	 */
	@Nullable
	default ResolvedInvokeDynamic resolveWithoutFrame(@Nonnull JvmTransformerContext context,
	                                                  @Nonnull Workspace workspace,
	                                                  @Nonnull ClassNode classNode,
	                                                  @Nonnull MethodNode method,
	                                                  @Nonnull InvokeDynamicInsnNode instruction) {
		return null;
	}

	/**
	 * Direct member target recovered from an {@code invokedynamic} call site.
	 *
	 * @param target
	 * 		Direct field or method handle to emit.
	 * @param ignoredTrailingArgumentCount
	 * 		Number of trailing dynamic call-site arguments that are metadata-only and must be removed.
	 */
	record ResolvedInvokeDynamic(@Nonnull Handle target, int ignoredTrailingArgumentCount) {
		public ResolvedInvokeDynamic {
			if (ignoredTrailingArgumentCount < 0)
				throw new IllegalArgumentException("Must provide non-negative trailing arg count");
		}
	}
}
