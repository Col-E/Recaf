package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;

import static org.objectweb.asm.Type.INT_TYPE;

/**
 * Executor for pushing array length values to the stack.
 *
 * @author Matt Coley
 */
public class ArrayLengthExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		// Get array size if possible
		Value stackTop = frame.pop();
		Value.NumericValue length;
		if (stackTop instanceof Value.ArrayValue) {
			Value[] array = ((Value.ArrayValue) stackTop).getArray();
			if (array != null) {
				length = new Value.NumericValue(INT_TYPE, array.length);
			} else {
				length = new Value.NumericValue(INT_TYPE);
			}
		} else {
			// Unknown length due to non-array value
			length = new Value.NumericValue(INT_TYPE);
			frame.markWonky("'arraylength' usage on non-array value");
		}
		frame.push(length);
	}
}
