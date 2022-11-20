package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.TypeInstruction;

import static org.objectweb.asm.Type.INT_TYPE;

/**
 * Executor for object type casting.
 *
 * @author Matt Coley
 */
public class InstanceofExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		if (!(instruction instanceof TypeInstruction))
			throw new AnalysisException(instruction, "Expected instanceof insn");

		// Stack value should be an object/array
		Value value = frame.pop();
		if (!value.isObject() && !value.isArray() && !value.isNull())
			frame.markWonky("instanceof expected an object, array, or null reference on the stack");

		// TODO: We can have a type-checker to know for certain (push 0 or 1)
		//  - useful for determining if the check is redundant
		frame.push(new Value.NumericValue(INT_TYPE));
	}
}
