package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.IndyInstruction;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Type;

import static me.coley.recaf.assemble.analysis.insn.InvokeExecutor.checkNumeric;
import static org.objectweb.asm.Type.*;

/**
 * Executor for {@code invokedynamic}.
 *
 * @author Matt Coley
 */
public class InvokeDynamicExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		if (!(instruction instanceof IndyInstruction))
			throw new AnalysisException(instruction, "Expected invokedynamic insn");
		// Same handling as an invoke-static
		IndyInstruction indyInstruction = (IndyInstruction) instruction;
		String desc = indyInstruction.getDesc();
		Type type = Type.getMethodType(desc);
		Type[] argTypes = type.getArgumentTypes();
		for (int i = argTypes.length - 1; i >= 0; i--) {
			// Iterating backwards so arguments are popped off stack in correct order.
			Type argType = argTypes[i];
			Value value;
			if (Types.isWide(argType))
				value = frame.popWide();
			else
				value = frame.pop();
			// Check type compatibility.
			switch (argType.getSort()) {
				case BOOLEAN:
				case CHAR:
				case BYTE:
				case SHORT:
				case INT:
					checkNumeric(frame, i, argType, value, sort -> sort <= INT);
					break;
				case FLOAT:
					checkNumeric(frame, i, argType, value, sort -> sort == FLOAT);
					break;
				case LONG:
					checkNumeric(frame, i, argType, value, sort -> sort == LONG);
					break;
				case DOUBLE:
					checkNumeric(frame, i, argType, value, sort -> sort == DOUBLE);
					break;
				case ARRAY:
				case OBJECT:
					if (!value.isNull() && !value.isObject() && !value.isArray())
						frame.markWonky("Dynamic argument " + i + " expected object value");
					break;
				default:
					break;
			}
		}
		Type retType = type.getReturnType();
		if (Types.isVoid(retType)) {
			// nothing
		} else if (retType.getSort() <= Type.DOUBLE) {
			frame.push(new Value.NumericValue(retType));
			if (Types.isWide(retType))
				frame.push(new Value.WideReservedValue());
		} else if (retType.getSort() == ARRAY) {
			frame.push(new Value.ArrayValue(retType.getDimensions(), retType.getElementType()));
		} else {
			frame.push(new Value.ObjectValue(retType));
		}
	}
}
