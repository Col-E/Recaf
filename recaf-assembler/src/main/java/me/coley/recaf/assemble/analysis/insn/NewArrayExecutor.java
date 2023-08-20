package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.NewArrayInstruction;
import org.objectweb.asm.Type;

/**
 * Executor for creating new arrays for primitive types.
 *
 * @author Matt Coley
 * @see NewObjectArrayExecutor For object arrays.
 */
public class NewArrayExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		// Get array type
		NewArrayInstruction newArrayInstruction = (NewArrayInstruction) instruction;
		Type elementType = Type.getType(String.valueOf(newArrayInstruction.getArrayTypeChar()));

		// Get array size, if possible
		Value.ArrayValue arrayValue;
		Value stackTop = frame.pop();
		if (stackTop instanceof Value.NumericValue) {
			Number size = ((Value.NumericValue) stackTop).getNumber();
			if (size == null) {
				arrayValue = new Value.ArrayValue(1, elementType);
			} else {
				arrayValue = new Value.ArrayValue(1, size.intValue(), elementType);

				// We can fill in for default values
				if (elementType.getSort() <= Type.DOUBLE)
					for (int i = 0; i < size.intValue(); i++)
						arrayValue.getArray()[i] = new Value.NumericValue(elementType, 0);
			}

		} else {
			// Unknown size due to non-numeric value
			arrayValue = new Value.ArrayValue(1, elementType);
			frame.markWonky("cannot compute array size, stack top value is non-numeric");
		}

		frame.push(arrayValue);
	}
}
