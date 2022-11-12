package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;

/**
 * Executor for duplicating a single value on the stack three slots down.
 *
 * @author Matt Coley
 */
public class DupX2Executor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		Value value1 = frame.pop();
		Value value2 = frame.pop();
		Value value3 = frame.pop();
		frame.push(value1); // inserted
		frame.push(value3); // original
		frame.push(value2); // original
		frame.push(value1); // original
		// Since this operation is on a single value, we cannot deal with wide-related values.
		if (value1.isWideNumeric()) {
			frame.markWonky("Cannot 'dup_x1' a wide stack value");
		} else if (value1.isWideReserved()) {
			frame.markWonky("Cannot 'dup_x1' a wide-reserved stack value");
		}
	}
}
