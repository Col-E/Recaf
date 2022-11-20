package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;

/**
 * Executor for popping a single value off the stack.
 *
 * @author Matt Coley
 */
public class PopExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		Value value = frame.pop();
		if (value.isWideReserved()) {
			frame.markWonky("Cannot 'pop' wide-reserved stack values");
		} else if (value.isWideNumeric()) {
			frame.markWonky("Cannot 'pop' wide value types (double/long)");
		}
	}
}
