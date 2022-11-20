package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;

/**
 * Executor for popping two values <i>(or one wide)</i> off the stack.
 *
 * @author Matt Coley
 */
public class Pop2Executor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		Value value = frame.popWide();
		// Check for case like:
		//  double
		//  reserved
		//  int (pop2)
		if (value.isWideReserved()) {
			frame.markWonky("'pop2' intrudes on a wide-reserved stack value");
		}
	}
}
