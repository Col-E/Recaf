package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;

/**
 * Executor for duplicating two values <i>(or one wide)</i> on the stack, three slots down.
 *
 * @author Matt Coley
 */
public class Dup2X2Executor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		Value value1 = frame.pop();
		Value value2 = frame.pop();
		Value value3 = frame.pop();
		Value value4 = frame.pop();
		frame.push(value2); // inserted
		frame.push(value1); // inserted
		frame.push(value4); // original
		frame.push(value3); // original
		frame.push(value2); // original
		frame.push(value1); // original
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
