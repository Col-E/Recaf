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
		Type type = Type.getObjectType(newArrayInstruction.getType());
		// Get array size, if possible
		Value.ArrayValue arrayValue;
		Value stackTop = frame.pop();
		if (stackTop instanceof Value.NumericValue) {
			Number size = ((Value.NumericValue) stackTop).getNumber();
			if (size == null) {
				arrayValue = new Value.ArrayValue(1, type);
			} else {
				arrayValue = new Value.ArrayValue(1, size.intValue(), type);
			}
		} else {
			// Unknown size due to non-numeric value
			arrayValue = new Value.ArrayValue(1, type);
			frame.markWonky("cannot compute array dimensions, stack top value is non-numeric");
		}
		frame.push(arrayValue);
	}
}
