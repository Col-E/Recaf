package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.TypeInstruction;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Type.ARRAY;

/**
 * Executor for object type casting.
 *
 * @author Matt Coley
 */
public class CheckcastExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		if (!(instruction instanceof TypeInstruction))
			throw new AnalysisException(instruction, "Expected checkcast insn");

		// Stack value should be an object/array
		Value value = frame.pop();
		if (!value.isObject() && !value.isArray() && !value.isNull())
			frame.markWonky("checkcast expected an object or array reference on the stack");

		// Replace top stack value with cast type. Otherwise, it's a ClassCastException.
		TypeInstruction typeInstruction = (TypeInstruction) instruction;

		// Type will either be internal name, or an array descriptor
		String typeStr = typeInstruction.getType();
		Type type = typeStr.charAt(0) == '[' ? Type.getType(typeStr) : Type.getObjectType(typeStr);
		if (type.getSort() == ARRAY) {
			frame.push(new Value.ArrayValue(type.getDimensions(), type.getElementType()));
		} else {
			frame.push(new Value.ObjectValue(type));
		}
	}
}
