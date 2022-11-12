package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;

/**
 * Executor for duplicating two values <i>(or one wide)</i> on the stack.
 *
 * @author Matt Coley
 */
public class Dup2Executor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		Value value1 = frame.peek();
		Value value2 = frame.peek(1);
		frame.push(value2);
		frame.push(value1);
		// Ensure (wide-value, reserved)
		if (value1.isWideReserved()) {
			if (!value2.isNumeric()) {
				frame.markWonky("Stack top is wide-reserved value, but next stack value is not a number");
			} else if (!value2.isWideNumeric()) {
				frame.markWonky("Stack top is wide-reserved value, but next stack value is not a wide value");
			}
		}
	}
}
