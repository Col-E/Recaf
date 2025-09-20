package software.coley.recaf.util.analysis;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.ReValue;

/**
 * Analysis frame for some enhanced value tracking.
 *
 * @author Matt Coley
 */
public class ReFrame extends Frame<ReValue> {
	/**
	 * New frame with the given number of expected locals/stack-slots.
	 *
	 * @param numLocals
	 * 		Available local variable slots.
	 * @param maxStack
	 * 		Available stack slots.
	 */
	public ReFrame(int numLocals, int maxStack) {
		super(numLocals, maxStack);
	}

	/**
	 * New frame copying the state of the given frame.
	 *
	 * @param frame
	 * 		Frame to copy stack/locals of.
	 */
	public ReFrame(@Nonnull Frame<? extends ReValue> frame) {
		super(frame);
	}

	@Override
	public void execute(AbstractInsnNode insn, Interpreter<ReValue> interpreter) throws AnalyzerException {
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
}
