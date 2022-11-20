package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.IincInstruction;
import me.coley.recaf.util.NumberUtil;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Type.INT;
import static org.objectweb.asm.Type.INT_TYPE;

/**
 * Executor for incrementing integer local variable values.
 *
 * @author Matt Coley
 */
public class IincExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		if (!(instruction instanceof IincInstruction))
			throw new AnalysisException(instruction, "Expected iinc insn");
		IincInstruction iincInstruction = (IincInstruction) instruction;
		String varName = iincInstruction.getName();
		Value value = frame.getLocal(varName);
		// Only update the variable if we're tracking the local's exact numeric value
		if (value instanceof Value.NumericValue) {
			Value.NumericValue numericValue = (Value.NumericValue) value;
			Type type = numericValue.getType();
			if (type.getSort() > INT) {
				frame.markWonky("Tried to increment non-int value from '" + varName + "'" +
						" got: " + type.getClassName());
				return;
			} else if (!numericValue.isPrimitive()) {
				frame.markWonky("Tried to increment boxed value from '" + varName + "'");
				return;
			}
			Number numValue = numericValue.getNumber();
			if (numValue != null) {
				int incr = iincInstruction.getIncrement();
				frame.setLocal(varName, new Value.NumericValue(INT_TYPE, NumberUtil.add(numValue, incr)));
			}
		} else if (value == null) {
			frame.markWonky("Tried to increment value from uninitialized '" + varName + "'");
		}
	}
}
