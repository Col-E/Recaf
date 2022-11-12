package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.IntInstruction;
import org.objectweb.asm.Type;

/**
 * Executor for pushing int values onto the stack.
 * <br>
 * Use by {@code XPUSH} instructions.
 *
 * @author Matt Coley
 */
public class IntPushExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		if (instruction instanceof IntInstruction) {
			int value = ((IntInstruction) instruction).getValue();
			frame.push(new Value.NumericValue(Type.INT_TYPE, value));
		} else {
			throw new AnalysisException(instruction, "Expected int insn");
		}
	}
}
