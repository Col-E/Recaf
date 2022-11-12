package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;

/**
 * Executor for monitor enter/exit.
 *
 * @author Matt Coley
 */
public class MonitorExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		String op = instruction.getOpcode();
		Value value = frame.pop();
		if (value.isNull()) {
			frame.markWonky("Cannot use " + op + " on null");
		} else if (!value.isObject()) {
			frame.markWonky(op + " expected object reference on stack");
		}
	}
}
