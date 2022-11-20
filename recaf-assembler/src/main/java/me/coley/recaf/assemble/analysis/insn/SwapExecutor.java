package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;

/**
 * Executor for swapping two stack values.
 *
 * @author Matt Coley
 */
public class SwapExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		Value value1 = frame.pop();
		Value value2 = frame.pop();
		checkAllowed(frame, value1);
		checkAllowed(frame, value2);
		frame.push(value1);
		frame.push(value2);
	}

	private static void checkAllowed(Frame frame, Value value) {
		if (value.isWideReserved()) {
			frame.markWonky("Cannot 'swap' wide-reserved stack values");
		} else if (value.isWideNumeric()) {
			frame.markWonky("Cannot 'swap' wide value types (double/long)");
		}
	}
}
