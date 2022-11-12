package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

/**
 * Executor for validating return instruction stack operands.
 *
 * @author Matt Coley
 */
public class ReturnExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		int op = instruction.getOpcodeVal();
		if (op == RETURN)
			return;

		// Handle typed returns
		boolean isWide = op == DRETURN || op == LRETURN;
		Value value = isWide ? frame.popWide() : frame.pop();

		// Check for object-return
		if (op == ARETURN) {
			if (!value.isObject() && !value.isNull() && !value.isArray())
				frame.markWonky("areturn yielded non-object value");
			return;
		}

		// Return value must be numeric
		Value.NumericValue numericValue = (Value.NumericValue) value;
		Type numericType = numericValue.getType();
		String numericTypeName = numericType.getClassName();
		int numericSort = numericType.getSort();
		switch (op) {
			case IRETURN:
				if (numericSort > Type.INT)
					frame.markWonky("ireturn yielded non-integer value: " + numericTypeName);
				break;
			case FRETURN:
				if (numericSort != Type.FLOAT)
					frame.markWonky("freturn yielded non-float value: " + numericTypeName);
				break;
			case DRETURN:
				if (numericSort != Type.DOUBLE)
					frame.markWonky("dreturn yielded non-double value: " + numericTypeName);
				break;
			case LRETURN:
				if (numericSort != Type.LONG)
					frame.markWonky("lreturn yielded non-long value: " + numericTypeName);
				break;
		}
	}
}
