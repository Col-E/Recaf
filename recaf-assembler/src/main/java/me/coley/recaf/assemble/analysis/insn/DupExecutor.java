package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;

/**
 * Executor for duplicating a single value on the stack.
 *
 * @author Matt Coley
 */
public class DupExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		Value value = frame.peek();
		frame.push(value);
		// Check for case like:
		//  double
		//  reserved (dup)
		if (value.isWideReserved()) {
			frame.markWonky("Cannot 'dup' a wide-reserved stack value");
		}
	}
}
