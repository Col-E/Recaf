package software.coley.recaf.util.analysis;

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
	public ReFrame(int numLocals, int maxStack) {
		super(numLocals, maxStack);
	}

	public ReFrame(Frame<? extends ReValue> frame) {
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
				if (updatedArray != array) {
					for (int i = 0; i < getStackSize(); i++) {
						ReValue stack = getStack(i);
						if (stack == array) {
							setStack(i, updatedArray);
						} else if (stack instanceof ArrayValue stackArray) {
							ArrayValue updatedStackArray = stackArray.updatedCopyIfContained(array, updatedArray);
							setStack(i, updatedStackArray);
						}
					}
					for (int i = 0; i < getLocals(); i++) {
						ReValue local = getLocal(i);
						if (local == array) {
							setLocal(i, updatedArray);
						} else if (local instanceof ArrayValue stackArray) {
							ArrayValue updatedStackArray = stackArray.updatedCopyIfContained(array, updatedArray);
							setLocal(i, updatedStackArray);
						}
					}
				}
				break;
			default:
				super.execute(insn, interpreter);
		}
	}
}
