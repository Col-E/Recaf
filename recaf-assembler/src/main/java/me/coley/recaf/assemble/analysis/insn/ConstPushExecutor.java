package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;

/**
 * Executor for pushing constant values onto the stack.
 * <br>
 * Use by {@code XCONST_X} instructions.
 *
 * @author Matt Coley
 */
public class ConstPushExecutor implements InstructionExecutor {
	private static final Value WIDE_RESERVED = new Value.WideReservedValue();
	private final Value constValue;

	/**
	 * @param constValue
	 * 		Value to push.
	 */
	public ConstPushExecutor(Value constValue) {
		this.constValue = constValue;
	}

	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		frame.push(constValue);
		if (constValue instanceof Value.NumericValue) {
			if (((Value.NumericValue) constValue).requireWidePadding()) {
				frame.push(WIDE_RESERVED);
			}
		}
	}
}
