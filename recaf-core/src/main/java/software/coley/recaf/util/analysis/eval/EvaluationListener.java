package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.util.ClassMethodPair;
import software.coley.recaf.util.analysis.ReFrame;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.UninitializedValue;

import java.util.List;

/**
 * Listener for successfully executed evaluator instructions and method lifecycle events
 * <i>(Block evaluations do not call lifecycle methods)</i>.
 *
 * @author Matt Coley
 */
@FunctionalInterface
public interface EvaluationListener {
	/**
	 * Called after each instruction is executed by the {@link Evaluator}.
	 * <p>
	 * For normal method evaluation, including nested workspace methods and class initializers, both {@code classNode}
	 * and {@code methodNode} are non-null. For instruction-block evaluation, both are {@code null} so block-only observations
	 * can be skipped with a simple {@code null} check.
	 * <p>
	 * The frame is the live executing frame after the instruction and is reused and mutated by subsequent steps.
	 * Listeners should not retain or mutate it <i>(footgun)</i>. Consumers that need history should copy it with the existing
	 * {@link ReFrame} copy constructor.
	 *
	 * @param classNode
	 * 		Class being evaluated, or {@code null} for block evaluation.
	 * @param methodNode
	 * 		Method being evaluated, or {@code null} for block evaluation.
	 * @param instruction
	 * 		Instruction that just executed.
	 * @param frame
	 * 		Live executing frame after the instruction.
	 */
	void onInstruction(@Nullable ClassNode classNode,
	                   @Nullable MethodNode methodNode,
	                   @Nonnull AbstractInsnNode instruction,
	                   @Nonnull ReFrame frame);

	/**
	 * Called when a workspace method begins execution.
	 * <p>
	 * The stack is an immutable snapshot ordered from the externally requested target method to the currently
	 * evaluated method. Nested method evaluations and class-initializer evaluations append to the stack, so the
	 * current method is always the last entry.
	 * <p>
	 * The frame is the live executing frame at method entry and is reused and mutated by subsequent steps.
	 * Listeners should not retain or mutate it <i>(footgun)</i>. Consumers that need history should copy it with the existing
	 * {@link ReFrame} copy constructor.
	 *
	 * @param classNode
	 * 		Class defining the method.
	 * @param methodNode
	 * 		Method beginning execution.
	 * @param frame
	 * 		Live executing frame at method entry.
	 * @param stack
	 * 		Immutable snapshot of the root-relative method call stack, including the current method.
	 */
	default void onMethodEnter(@Nonnull ClassNode classNode, @Nonnull MethodNode methodNode,
	                           @Nonnull ReFrame frame, @Nonnull List<ClassMethodPair> stack) {
		// no-op
	}

	/**
	 * Called after a workspace method completes with a normal return.
	 * <p>
	 * The stack is an immutable snapshot ordered from the externally requested target method to the currently
	 * evaluated method. Nested method evaluations and class-initializer evaluations append to the stack, so the
	 * current method is always the last entry. Void methods report {@link UninitializedValue#UNINITIALIZED_VALUE}
	 * as their value.
	 * <p>
	 * The frame is the live executing frame at method completion and is reused and mutated by subsequent steps.
	 * Listeners should not retain or mutate it <i>(footgun)</i>. Consumers that need history should copy it with the existing
	 * {@link ReFrame} copy constructor.
	 *
	 * @param classNode
	 * 		Class that defined the completed method.
	 * @param methodNode
	 * 		Completed method.
	 * @param frame
	 * 		Live executing frame at method completion.
	 * @param value
	 * 		Normalized method return value.
	 * @param stack
	 * 		Immutable snapshot of the root-relative method call stack, including the current method.
	 */
	default void onMethodReturn(@Nonnull ClassNode classNode, @Nonnull MethodNode methodNode,
	                            @Nonnull ReFrame frame, @Nonnull ReValue value,
	                            @Nonnull List<ClassMethodPair> stack) {
		// no-op
	}

	/**
	 * Called after a workspace method exits with an uncaught evaluator exception.
	 * <p>
	 * The stack is an immutable snapshot ordered from the externally requested target method to the currently
	 * evaluated method. Nested method evaluations and class-initializer evaluations append to the stack, so the
	 * current method is always the last entry. Exceptions handled within the method do not emit this callback because
	 * the method continues executing.
	 * <p>
	 * The frame is the live executing frame at the uncaught exception and is reused and mutated by subsequent steps.
	 * Listeners should not retain or mutate it <i>(footgun)</i>. Consumers that need history should copy it with the existing
	 * {@link ReFrame} copy constructor.
	 *
	 * @param classNode
	 * 		Class that defined the throwing method.
	 * @param methodNode
	 * 		Throwing method.
	 * @param frame
	 * 		Live executing frame at the uncaught exception.
	 * @param exception
	 * 		Exception value that left the method.
	 * @param stack
	 * 		Immutable snapshot of the root-relative method call stack, including the current method.
	 */
	default void onMethodThrow(@Nonnull ClassNode classNode, @Nonnull MethodNode methodNode,
	                           @Nonnull ReFrame frame, @Nonnull ReValue exception,
	                           @Nonnull List<ClassMethodPair> stack) {
		// no-op
	}
}
