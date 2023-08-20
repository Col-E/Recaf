package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.NewArrayInstruction;
import me.coley.recaf.assemble.ast.insn.TypeInstruction;
import org.objectweb.asm.Type;

/**
 * Executor for creating new arrays for object types.
 *
 * @author Matt Coley
 * @see NewArrayInstruction For primitive arrays.
 */
public class NewObjectArrayExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		// Get array type
		TypeInstruction newArrayInstruction = (TypeInstruction) instruction;
		String newArrayType = newArrayInstruction.getType();
		boolean isCreatingArrayOfArrays = newArrayType.charAt(0) == '[';
		Type elementType = isCreatingArrayOfArrays ?
				Type.getType(newArrayType).getElementType() : Type.getObjectType(newArrayType);
		int dimensions = isCreatingArrayOfArrays ? Type.getType(newArrayType).getDimensions() +1: 1;

		// Get array size, if possible
		Value.ArrayValue arrayValue;
		Value stackTop = frame.pop();
		if (stackTop instanceof Value.NumericValue) {
			Number size = ((Value.NumericValue) stackTop).getNumber();
			if (size == null) {
				arrayValue = new Value.ArrayValue(dimensions, elementType);
			} else {
				arrayValue = new Value.ArrayValue(dimensions, size.intValue(), elementType);

				// We can fill in for default values
				if (elementType.getSort() <= Type.DOUBLE)
					for (int i = 0; i < size.intValue(); i++)
						arrayValue.getArray()[i] = new Value.NullValue();
			}
		} else {
			// Unknown size due to non-numeric value
			arrayValue = new Value.ArrayValue(1, elementType);
			frame.markWonky("cannot compute array dimensions, stack top value is non-numeric");
		}
		frame.push(arrayValue);
	}
}
