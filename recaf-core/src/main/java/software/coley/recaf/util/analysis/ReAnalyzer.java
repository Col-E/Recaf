package software.coley.recaf.util.analysis;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import software.coley.recaf.util.analysis.value.ReValue;

import java.util.function.Consumer;

/**
 * Analyzer that takes in an interpreter for {@link ReValue enhanced value types}.
 *
 * @author Matt Coley
 */
public class ReAnalyzer extends Analyzer<ReValue> {
	private final ReInterpreter interpreter;
	private MethodNode targetMethod;
	private AbstractInsnNode currentInsn;

	/**
	 * @param interpreter
	 * 		Enhanced interpreter.
	 */
	public ReAnalyzer(@Nonnull ReInterpreter interpreter) {
		super(interpreter);
		this.interpreter = interpreter;
	}

	/**
	 * @return Interpreter backing this analyzer.
	 */
	@Nonnull
	public ReInterpreter getInterpreter() {
		return interpreter;
	}

	@Override
	protected ReFrame newFrame(int numLocals, int numStack) {
		return new ReFrame(this, numLocals, numStack);
	}

	@Override
	protected ReFrame newFrame(@Nonnull Frame<? extends ReValue> frame) {
		return new ReFrame(this, frame);
	}

	@Override
	protected void init(@Nullable String owner, @Nonnull MethodNode method) throws AnalyzerException {
		targetMethod = method;
		fillJumpTargetTemplateFrames();
	}

	@Override
	public Frame<ReValue>[] analyze(@Nonnull String owner, @Nonnull MethodNode method) throws AnalyzerException {
		// Process the method's frames.
		Frame<ReValue>[] frames = super.analyze(owner, method);

		// Remove template and frames never branched to.
		int frameCount = frames.length;
		for (int i = 0; i < frameCount; i++) {
			ReFrame frame = (ReFrame) frames[i];
			if (frame != null && (frame.isTemplate() || frame.isNeverBranchedTo()))
				frames[i] = null;
		}

		return frames;
	}

	/**
	 * Fills in all jump/switch targets with template frames.
	 * These templates exist so that {@code Analyzer#merge(int, Frame, Subroutine)}
	 * will call the template frame's {@link Frame#merge(Frame, Interpreter)}
	 * instead of creating a new frame.
	 * <p/>
	 * When we control the frame's merge function we can hint to the analyzer
	 * that we should not process following instructions if merge yields {@code false}.
	 * With this, we can track when the frames of jump destinations will never be visited
	 * and then remove those frames once analysis completes. A missing frame implies the
	 * code is unreachable, which is our desired outcome for jump targets that we know
	 * for sure will never be visited <i>(like {@code iconst_1, ifeq neverVisited})</i>.
	 */
	protected void fillJumpTargetTemplateFrames() {
		InsnList instructions = targetMethod.instructions;
		Frame<ReValue>[] frames = getFrames();
		Consumer<AbstractInsnNode> frameFill = insn -> {
			if (insn == null) return;
			int index = instructions.indexOf(insn);
			if (index > 0 && index < frames.length) {
				ReFrame frame = newFrame(targetMethod.maxLocals, targetMethod.maxStack);
				frame.markTemplate();
				frames[index] = frame;
			}
		};
		for (AbstractInsnNode instruction : instructions) {
			if (instruction instanceof JumpInsnNode jin) {
				frameFill.accept(jin.label);
				int opcode = jin.getOpcode();
				if (opcode != GOTO && opcode != JSR)
					frameFill.accept(jin.getNext());
			} else if (instruction instanceof TableSwitchInsnNode tswitch) {
				frameFill.accept(tswitch.dflt);
				tswitch.labels.forEach(frameFill);
			} else if (instruction instanceof LookupSwitchInsnNode lswitch) {
				frameFill.accept(lswitch.dflt);
				lswitch.labels.forEach(frameFill);
			}
		}
	}

	/**
	 * Called by {@link ReFrame#execute(AbstractInsnNode, Interpreter)}.
	 *
	 * @param insn
	 * 		Current instruction being executed.
	 */
	protected void notifyCurrentInsn(@Nonnull AbstractInsnNode insn) {
		currentInsn = insn;
	}

	/**
	 * Called by {@link ReFrame#initJumpTarget(int, LabelNode)}.
	 *
	 * @param target
	 * 		The target of the jump, or {@code null} for contol flow fall-through.
	 * @param branching
	 * 		The branching behavior of the jump.
	 * 		Can be {@link Branching#TAKEN} or {@link Branching#NOT_TAKEN} for cases where we know for sure,
	 * 		or {@link Branching#UNKNOWN} for cases where we do not know for sure.
	 */
	protected void notifyJumpVisited(@Nullable LabelNode target, @Nonnull Branching branching) {
		if (target == null) {
			// Get target frame index for fall-through.
			AbstractInsnNode next = currentInsn.getNext();
			if (next == null)
				return;
			int targetIndex = targetMethod.instructions.indexOf(next);
			if (targetIndex < 0)
				return;

			// Record inverse branching behavior for fall-through target.
			ReFrame targetFrame = (ReFrame) getFrames()[targetIndex];
			targetFrame.addBranchingBehavior(branching.invert());
		} else {
			int targetIndex = targetMethod.instructions.indexOf(target);
			if (targetIndex < 0)
				return;

			// Record branching behavior for jump target.
			ReFrame targetFrame = (ReFrame) getFrames()[targetIndex];
			targetFrame.addBranchingBehavior(branching);
		}
	}
}
