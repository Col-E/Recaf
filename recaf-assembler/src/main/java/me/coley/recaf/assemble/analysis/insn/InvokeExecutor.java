package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.MethodInstruction;
import me.coley.recaf.util.OpcodeUtil;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Type;

import java.util.function.IntPredicate;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Type.*;

/**
 * Executor for {@code invokeX} instructions.
 *
 * @author Matt Coley
 */
public class InvokeExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		if (!(instruction instanceof MethodInstruction))
			throw new AnalysisException(instruction, "Expected invokeX instruction");
		int op = instruction.getOpcodeVal();
		MethodInstruction methodInstruction = (MethodInstruction) instruction;
		String desc = methodInstruction.getDesc();
		if (!Types.isValidDesc(desc)) {
			frame.markWonky("Invalid method descriptor");
			return;
		}
		// Pop arguments off stack.
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
		// Pop method owner ctx off the stack.
		if (op != INVOKESTATIC) {
			Value owner = frame.pop();
			String opName = OpcodeUtil.opcodeToName(op).toLowerCase();
			if (owner.isEmptyStack()) {
				frame.markWonky(opName + " has no stack value to use as an 'owner'");
			} else if (owner.isNull()) {
				frame.markWonky(opName + " 'owner' on stack is null!");
			} else if (!owner.isObject()) {
				frame.markWonky(opName + " 'owner' on stack not an object type!");
			}
		}
		// Push return value.
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

	public static void checkNumeric(Frame frame, int argIndex, Type argType, Value value, IntPredicate sortPredicate) {
		if (!value.isNumeric()) {
			frame.markWonky("Invoke argument " + argIndex + " expected numeric value");
		} else {
			Value.NumericValue numericValue = (Value.NumericValue) value;
			Type valueType = numericValue.getType();
			if (!sortPredicate.test(valueType.getSort())) {
				frame.markWonky("Invoke argument " + argIndex + " expected " +
						argType.getClassName() + " value, got: " + valueType.getClassName());
			}
		}
	}
}
