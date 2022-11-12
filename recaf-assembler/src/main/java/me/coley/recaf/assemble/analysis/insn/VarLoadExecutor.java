package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.VarInstruction;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * Executor for pushing local variable values onto the stack.
 *
 * @author Matt Coley
 */
public class VarLoadExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		if (!(instruction instanceof VarInstruction))
			throw new AnalysisException(instruction, "Expected variable insn");
		VarInstruction varInstruction = (VarInstruction) instruction;
		int op = varInstruction.getOpcodeVal();
		String varName = varInstruction.getName();
		Value value = frame.getLocal(varName);
		if (value == null) {
			// Value not previously known, assume generic value based on type.
			frame.markWonky("Tried to load from uninitialized '" + varName + "'");
			switch (op) {
				case ILOAD:
					value = new Value.NumericValue(INT_TYPE);
					break;
				case LLOAD:
					value = new Value.NumericValue(LONG_TYPE);
					break;
				case FLOAD:
					value = new Value.NumericValue(FLOAT_TYPE);
					break;
				case DLOAD:
					value = new Value.NumericValue(DOUBLE_TYPE);
					break;
				case ALOAD:
					value = new Value.ObjectValue(Types.OBJECT_TYPE);
					break;
			}
		} else {
			// Value previously known, validate the load context is correct.
			if (op == ALOAD) {
				// 'aload' must operate on object types.
				if (!value.isNull() && !value.isObject() && !value.isArray())
					frame.markWonky("'aload' used on non-object value");
			} else if (op >= ILOAD && op <= DLOAD) {
				// Numeric loads must operate on numeric values.
				if (value instanceof Value.NumericValue) {
					Value.NumericValue numericValue = (Value.NumericValue) value;
					if (!numericValue.isPrimitive())
						frame.markWonky("'" + instruction.getOpcode() + "' on numeric value of boxed type");
					Type valueType = numericValue.getType();
					switch (op) {
						case ILOAD:
							if (valueType.getSort() > Type.INT)
								frame.markWonky("'iload' requires local value to be a boolean/char/byte/short/int, got: " + valueType.getClassName());
							break;
						case LLOAD:
							if (valueType.getSort() != Type.LONG)
								frame.markWonky("'lload' requires local value to be a long, got: " + valueType.getClassName());
							break;
						case FLOAD:
							if (valueType.getSort() != Type.FLOAT)
								frame.markWonky("'fload' requires local value to be a float, got: " + valueType.getClassName());
							break;
						case DLOAD:
							if (valueType.getSort() != Type.DOUBLE)
								frame.markWonky("'dload' requires local value to be a double, got: " + valueType.getClassName());
							break;
					}
				} else {
					switch (op) {
						case ILOAD:
							frame.markWonky("'iload' requires local value to be a boolean/char/byte/short/int");
							break;
						case LLOAD:
							frame.markWonky("'lload' requires local value to be a long");
							break;
						case FLOAD:
							frame.markWonky("'fload' requires local value to be a float");
							break;
						case DLOAD:
							frame.markWonky("'dload' requires local value to be a double");
							break;
					}
				}
			} else {
				throw new AnalysisException(instruction, "Expected XLOAD instruction");
			}
		}
		// Push local variable value onto stack
		frame.push(value);
		if (op == DLOAD || op == LLOAD)
			frame.push(new Value.WideReservedValue());
	}
}
