package software.coley.recaf.util.analysis;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;

import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Analysis frame for some enhanced value tracking.
 *
 * @author Matt Coley
 */
public class ReFrame extends Frame<ReValue> {
	private final ReAnalyzer analyzer;
	private final ReValue[] localsSnapshot;
	private final ReValue[] stackSnapshot;
	private int stackSnapshotSize;
	private int localsSnapshotSize;
	private Branching branchState;
	private boolean template;


	/**
	 * New frame with the given number of expected locals/stack-slots.
	 *
	 * @param analyzer
	 * 		Parent analyzer for additional control flow hinting. {@code null} to skip hinting process.
	 * @param numLocals
	 * 		Available local variable slots.
	 * @param maxStack
	 * 		Available stack slots.
	 */
	public ReFrame(@Nullable ReAnalyzer analyzer, int numLocals, int maxStack) {
		super(numLocals, maxStack);
		this.analyzer = analyzer;
		localsSnapshot = new ReValue[numLocals];
		stackSnapshot = new ReValue[maxStack];
	}

	/**
	 * New frame copying the state of the given frame.
	 *
	 * @param analyzer
	 * 		Parent analyzer for additional control flow hinting. {@code null} to skip hinting process.
	 * @param frame
	 * 		Frame to copy stack/locals of.
	 */
	public ReFrame(@Nullable ReAnalyzer analyzer, @Nonnull Frame<? extends ReValue> frame) {
		super(frame);
		this.analyzer = analyzer;
		localsSnapshot = new ReValue[frame.getLocals()];
		stackSnapshot = new ReValue[frame.getMaxStackSize()];
	}

	@Override
	public void execute(@Nonnull AbstractInsnNode insn, @Nonnull Interpreter<ReValue> interpreter) throws AnalyzerException {
		// Update control flow hinting.
		if (analyzer != null) {
			// Notify parent where we are at in the method.
			analyzer.notifyCurrentInsn(insn);

			// Update branch state to indicate we've naturally flowed into this instruction.
			addBranchingBehavior(Branching.TAKEN);

			// Track the local/stack state before execution.
			updateLocalAndStackSnapshots();
		}

		// Handle specific instruction edge cases, or delegate to default execution handling.
		switch (insn.getOpcode()) {
			case Opcodes.IASTORE:
			case Opcodes.LASTORE:
			case Opcodes.FASTORE:
			case Opcodes.DASTORE:
			case Opcodes.AASTORE:
			case Opcodes.BASTORE:
			case Opcodes.CASTORE:
			case Opcodes.SASTORE:
				// Support updating array contents in this frame when values are stored into it.
				ReValue value = pop();
				ReValue index = pop();
				ReValue array = pop();
				ReValue updatedArray = interpreter.ternaryOperation(insn, array, index, value);
				if (updatedArray != array)
					replaceValue(array, updatedArray);
				break;
			default:
				super.execute(insn, interpreter);
		}
	}

	@Override
	public void initJumpTarget(int opcode, @Nullable LabelNode target) {
		// No parent? Not able to share flow control hints.
		if (analyzer == null)
			return;

		// Notify the parent analyzer of which jumps are taken.
		switch (opcode) {
			case Opcodes.IFEQ -> notifyUnary(target, top -> top.isEqualTo(0));
			case Opcodes.IFNE -> notifyUnary(target, top -> top.isNotEqualTo(0));
			case Opcodes.IFLT -> notifyUnary(target, top -> top.isLessThan(0));
			case Opcodes.IFGE -> notifyUnary(target, top -> top.isGreaterThanOrEqual(0));
			case Opcodes.IFGT -> notifyUnary(target, top -> top.isGreaterThan(0));
			case Opcodes.IFLE -> notifyUnary(target, top -> top.isLessThanOrEqual(0));
			case Opcodes.IFNULL -> notifyUnaryNullness(target, top ->
					top.nullness() == Nullness.UNKNOWN ?
							Branching.UNKNOWN :
							top.isNull() ?
									Branching.TAKEN : Branching.NOT_TAKEN);
			case Opcodes.IFNONNULL -> notifyUnaryNullness(target, top ->
					top.nullness() == Nullness.UNKNOWN ?
							Branching.UNKNOWN :
							top.isNotNull() ?
									Branching.TAKEN : Branching.NOT_TAKEN);
			case Opcodes.IF_ICMPEQ -> notifyBinary(target, IntValue::isEqualTo);
			case Opcodes.IF_ICMPNE -> notifyBinary(target, IntValue::isNotEqualTo);
			case Opcodes.IF_ICMPLT -> notifyBinary(target, IntValue::isLessThan);
			case Opcodes.IF_ICMPGE -> notifyBinary(target, IntValue::isGreaterThanOrEqual);
			case Opcodes.IF_ICMPGT -> notifyBinary(target, IntValue::isGreaterThan);
			case Opcodes.IF_ICMPLE -> notifyBinary(target, IntValue::isLessThanOrEqual);
			case Opcodes.IF_ACMPEQ -> notifyBinaryRefs(target, (l, r) ->
					l.nullness() == Nullness.UNKNOWN || r.nullness() == Nullness.UNKNOWN ?
							Branching.UNKNOWN :
							l == r ?
									Branching.TAKEN :
									Branching.UNKNOWN);
			case Opcodes.IF_ACMPNE -> notifyBinaryRefs(target, (l, r) ->
					l.nullness() == Nullness.UNKNOWN || r.nullness() == Nullness.UNKNOWN ?
							Branching.UNKNOWN :
							l == r ?
									Branching.NOT_TAKEN :
									Branching.UNKNOWN);
			default -> {}
		}
	}

	private void notifyUnary(@Nullable LabelNode target, @Nonnull Predicate<IntValue> predicate) {
		ReValue top = stackSnapshot[stackSnapshotSize - 1];
		if (top.hasKnownValue() && top instanceof IntValue intTop) {
			Branching branching = predicate.test(intTop) ? Branching.TAKEN : Branching.NOT_TAKEN;
			if (target == null) // Fall through is taken only if the branch predicate fails, so invert the test result.
				branching = branching.invert();
			analyzer.notifyJumpVisited(target, branching);
		} else {
			analyzer.notifyJumpVisited(target, Branching.UNKNOWN);
		}
	}

	private void notifyUnaryNullness(@Nullable LabelNode target, @Nonnull UnBranchingComputer<ObjectValue> predicate) {
		ReValue top = stackSnapshot[stackSnapshotSize - 1];
		if (top instanceof ObjectValue objTop) {
			Branching branching = predicate.compute(objTop);
			if (target == null) // Fall through is taken only if the branch predicate fails, so invert the test result.
				branching = branching.invert();
			analyzer.notifyJumpVisited(target, branching);
		} else {
			analyzer.notifyJumpVisited(target, Branching.UNKNOWN);
		}
	}

	private void notifyBinary(@Nullable LabelNode target, @Nonnull BiPredicate<IntValue, IntValue> predicate) {
		ReValue topL = stackSnapshot[stackSnapshotSize - 2];
		ReValue topR = stackSnapshot[stackSnapshotSize - 1];
		if (topL.hasKnownValue() && topL instanceof IntValue intTopLeft &&
				topR.hasKnownValue() && topR instanceof IntValue intTopRight) {
			Branching branching = predicate.test(intTopLeft, intTopRight) ? Branching.TAKEN : Branching.NOT_TAKEN;
			if (target == null) // Fall through is taken only if the branch predicate fails, so invert the test result.
				branching = branching.invert();
			analyzer.notifyJumpVisited(target, branching);
		} else {
			analyzer.notifyJumpVisited(target, Branching.UNKNOWN);
		}
	}

	private void notifyBinaryRefs(@Nullable LabelNode target, @Nonnull BiBranchingComputer<ObjectValue> predicate) {
		ReValue topL = stackSnapshot[stackSnapshotSize - 2];
		ReValue topR = stackSnapshot[stackSnapshotSize - 1];
		if (topL.hasKnownValue() && topL instanceof ObjectValue objTopLeft &&
				topR.hasKnownValue() && topR instanceof ObjectValue objTopRight) {
			Branching branching = predicate.compute(objTopLeft, objTopRight);
			if (target == null) // Fall through is taken only if the branch predicate fails, so invert the test result.
				branching = branching.invert();
			analyzer.notifyJumpVisited(target, branching);
		} else {
			analyzer.notifyJumpVisited(target, Branching.UNKNOWN);
		}
	}

	@Override
	public boolean merge(@Nonnull Frame<? extends ReValue> frame, @Nonnull Interpreter<ReValue> interpreter) throws AnalyzerException {
		if (analyzer != null && template) {
			// Template frames must take a full copy of the originating frame.
			init(frame);
			template = false;

			// Quick return since we don't need to call super.merge()
			// since all values are the same as the originating frame.
			return !isNeverBranchedTo();
		}

		// We will merge this frame's values, but return false when the observed branching state is 'never taken'.
		// The branching state is updated when we handle execution of a branching instruction targeting this frame.
		// If we can asser that the branching behavior for this frame is 'never taken' then returning false here
		// will prevent the parent analyzer from continuing execution from this point.
		return super.merge(frame, interpreter);
	}

	private void updateLocalAndStackSnapshots() {
		int stackSnapshotSize = getStackSize();
		int localsSnapshotSize = getLocals();

		this.stackSnapshotSize = stackSnapshotSize;
		this.localsSnapshotSize = localsSnapshotSize;

		Arrays.fill(stackSnapshot, stackSnapshotSize, stackSnapshot.length, null);
		for (int i = 0; i < stackSnapshotSize; i++)
			stackSnapshot[i] = getStack(i);
		for (int i = 0; i < localsSnapshotSize; i++)
			localsSnapshot[i] = getLocal(i);
	}

	/**
	 * Replace all references to the given value with the replacement.
	 *
	 * @param existing
	 * 		Some existing value that exists in this frame.
	 * @param replacement
	 * 		Instance to replace the existing value with.
	 */
	public void replaceValue(@Nonnull ReValue existing, @Nonnull ReValue replacement) {
		for (int i = 0; i < getStackSize(); i++) {
			ReValue stack = getStack(i);
			if (stack == existing) {
				setStack(i, replacement);
			} else if (stack instanceof ArrayValue stackArray) {
				ArrayValue updatedStackArray = stackArray.updatedCopyIfContained(existing, replacement);
				setStack(i, updatedStackArray);
			}
		}
		for (int i = 0; i < getLocals(); i++) {
			ReValue local = getLocal(i);
			if (local == existing) {
				setLocal(i, replacement);
			} else if (local instanceof ArrayValue stackArray) {
				ArrayValue updatedStackArray = stackArray.updatedCopyIfContained(existing, replacement);
				setLocal(i, updatedStackArray);
			}
		}
	}

	/**
	 * @return {@code true} when this frame represents code that is never reached.
	 * {@code false} when this code is visited by any means.
	 */
	public boolean isNeverBranchedTo() {
		return branchState == Branching.NOT_TAKEN;
	}

	/**
	 * Record an instance of branching behavior. Merges the branching behavior state
	 * to a value encompassing all observed cases.
	 * <ul>
	 *     <li>Consistent {@link Branching} values yields the consistent value as the merged state.</li>
	 *     <li>Inconsistent {@link Branching} values yields {@link Branching#UNKNOWN}</li>
	 * </ul>
	 *
	 * @param branching
	 * 		Branching behavior observed in this frame.
	 */
	protected void addBranchingBehavior(@Nonnull Branching branching) {
		if (branchState == null)
			branchState = branching;
		else if (branching == Branching.UNKNOWN)
			branchState = Branching.UNKNOWN;
		else if (branchState == Branching.TAKEN && branching == Branching.NOT_TAKEN)
			branchState = Branching.UNKNOWN;
		else if (branchState == Branching.NOT_TAKEN && branching == Branching.TAKEN)
			branchState = Branching.UNKNOWN;
	}

	/**
	 * @return {@code true} when this frame is a placeholder for later control flow visitation.
	 *
	 * @see ReAnalyzer#init(String, MethodNode)
	 * @see #merge(Frame, Interpreter)
	 */
	public boolean isTemplate() {
		return template;
	}

	/**
	 * Marks this frame as being a template for later control flow visitation.
	 *
	 * @see ReAnalyzer#init(String, MethodNode)
	 * @see #merge(Frame, Interpreter)
	 */
	protected void markTemplate() {
		template = true;
	}

	@Override
	public String toString() {
		if (template) return "<template>";
		return super.toString();
	}

	private interface UnBranchingComputer<T extends ReValue> {
		@Nonnull
		Branching compute(@Nonnull T value);
	}

	private interface BiBranchingComputer<T extends ReValue> {
		@Nonnull
		Branching compute(@Nonnull T valueLeft, @Nonnull T valueRight);
	}
}
