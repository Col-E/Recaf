package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.MultiArrayInstruction;
import org.objectweb.asm.Type;

/**
 * Executor for pushing multi-dimensional object array values onto the stack.
 *
 * @author Matt Coley
 */
public class MultiNewArrayExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		if (!(instruction instanceof MultiArrayInstruction))
			throw new AnalysisException(instruction, "Expected multi-array insn");
		MultiArrayInstruction newArrayInstruction = (MultiArrayInstruction) instruction;
		Type elementType = Type.getType(newArrayInstruction.getDesc()).getElementType();
		int numDimensions = newArrayInstruction.getDimensions();

		// Create N-Dimensional array
		Value.ArrayValue arrayValue = new Value.ArrayValue(numDimensions, elementType);

		// Attempt to create correctly-sized sub-arrays from stack
		Value[] backingArray = arrayValue.getArray();
		for (int i = 0; i < numDimensions; i++) {
			Value stackTop = frame.pop();
			if (stackTop instanceof Value.NumericValue) {
				Number size = ((Value.NumericValue) stackTop).getNumber();
				if (size == null) {
					backingArray[i] = new Value.ArrayValue(numDimensions, elementType);
				} else {
					backingArray[i] = new Value.ArrayValue(numDimensions, size.intValue(), elementType);
				}
			} else {
				// Unknown size due to non-numeric value
				backingArray[i] = new Value.ArrayValue(numDimensions, elementType);
				frame.markWonky("cannot compute array dimensions, stack top value[" + i + "] is non-numeric");
			}
		}
		frame.push(arrayValue);
	}
}
