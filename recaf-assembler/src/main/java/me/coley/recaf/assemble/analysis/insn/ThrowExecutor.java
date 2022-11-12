package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;

/**
 * Executor for handling {@code ATHROW}.
 *
 * @author Matt Coley
 */
public class ThrowExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		// Top of stack is an object ref to an exception
		Value exceptionValue = frame.pop();
		if (!exceptionValue.isObject())
			frame.markWonky("athrow expected an object reference on the stack");
		// Throwing clears the stack
		while (!frame.getStack().isEmpty())
			frame.pop();
	}
}
