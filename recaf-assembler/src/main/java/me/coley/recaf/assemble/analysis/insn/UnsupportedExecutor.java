package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;

/**
 * Used for instructions that are not supported in analysis.
 * Primarily deprecated instructions.
 *
 * @author Matt Coley
 */
public class UnsupportedExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		throw new AnalysisException(instruction, "'" + instruction.getOpcode() + "' is not supported");
	}
}
