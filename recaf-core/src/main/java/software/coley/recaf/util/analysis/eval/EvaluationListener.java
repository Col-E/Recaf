package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.util.analysis.ReFrame;

/**
 * Listener for successfully executed evaluator instructions.
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
}
