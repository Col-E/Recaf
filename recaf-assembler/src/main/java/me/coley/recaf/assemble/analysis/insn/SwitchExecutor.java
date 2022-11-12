package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.CodeExecutor;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.LOOKUPSWITCH;
import static org.objectweb.asm.Opcodes.TABLESWITCH;

/**
 * Executor for validating switch instruction stack operands.
 * The actual branching logic is handled internally by {@link CodeExecutor#execute(int, int)}
 *
 * @author Matt Coley
 */
public class SwitchExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		int op = instruction.getOpcodeVal();
		if (op != TABLESWITCH && op != LOOKUPSWITCH) {
			throw new AnalysisException(instruction, "Unknown switch instruction: " + instruction.getOpcode());
		}
		String opName = instruction.getOpcode();

		Value value = frame.pop();
		if (!value.isNumeric()) {
			frame.markWonky(opName + " operand is non-numeric");
			return;
		}
		Value.NumericValue numericValue = (Value.NumericValue) value;
		Type numericType = numericValue.getType();
		if (numericType.getSort() > Type.INT) {
			frame.markWonky(opName + " operand must be an integer, was " + numericType.getClassName());
		}
	}
}
