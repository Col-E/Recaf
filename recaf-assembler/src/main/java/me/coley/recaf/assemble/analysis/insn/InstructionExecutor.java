package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;

/**
 * Base for instruction execution.
 *
 * @author Matt Coley
 */
public interface InstructionExecutor {
	/**
	 * @param frame
	 * 		Frame to operate on.
	 * @param instruction
	 * 		Instruction to execute.
	 *
	 * @throws AnalysisException
	 * 		Thrown when the instruction cannot be executed.
	 * 		Not to be confused with {@link Frame#markWonky(String)}. These are irrecoverable failures.
	 * 		Anything else will just mark the frame as wonky and attempt to continue.
	 */
	void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException;
}
