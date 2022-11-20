package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.FieldInstruction;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Type.ARRAY;

/**
 * Executor for field get and put instructions.
 *
 * @author Matt Coley
 */
public class FieldExecutor implements InstructionExecutor{
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		int op = instruction.getOpcodeVal();
		switch (op) {
			case GETFIELD: {
				// Pop field owner ctx
				Value owner = frame.pop();
				if (owner.isEmptyStack()) {
					frame.markWonky("getfield has no stack value to use as an 'owner'");
				} else if (owner.isNull()) {
					frame.markWonky("getfield 'owner' on stack is null!");
				} else if (!owner.isObject()) {
					frame.markWonky("getfield 'owner' on stack not an object type!");
				}
				// Fall through
			}
			case GETSTATIC: {
				// Push field value
				FieldInstruction fieldInstruction = (FieldInstruction) instruction;
				String desc = fieldInstruction.getDesc();
				Type type = Type.getType(desc);
				if (type.getSort() <= Type.DOUBLE) {
					frame.push(new Value.NumericValue(type));
					if (Types.isWide(type))
						frame.push(new Value.WideReservedValue());
				} else if (type.getSort() == ARRAY) {
					frame.push(new Value.ArrayValue(type.getDimensions(), type.getElementType()));
				} else {
					frame.push(new Value.ObjectValue(type));
				}
				break;
			}
			case PUTSTATIC: {
				// Pop value
				FieldInstruction fieldInstruction = (FieldInstruction) instruction;
				Type type = Type.getType(fieldInstruction.getDesc());
				Value value = Types.isWide(type) ? frame.popWide() : frame.pop();
				if (type.getSort() >= ARRAY && !(value.isNull() || value.isObject() || value.isArray())) {
					frame.markWonky("putstatic field is object/array, but value on stack is non-object");
				} else if (type.getSort() <= Type.DOUBLE && !value.isNumeric()) {
					frame.markWonky("putstatic field is numeric, but value on stack is non-numeric");
				}
				break;
			}
			case PUTFIELD: {
				// Pop value
				FieldInstruction fieldInstruction = (FieldInstruction) instruction;
				Type type = Type.getType(fieldInstruction.getDesc());
				Value value = Types.isWide(type) ? frame.popWide() : frame.pop();
				if (type.getSort() >= ARRAY && !(value.isNull() || value.isObject() || value.isArray())) {
					frame.markWonky("putfield field is object/array, but value on stack is non-object");
				} else if (type.getSort() <= Type.DOUBLE && !value.isNumeric()) {
					frame.markWonky("putfield field is numeric, but value on stack is non-numeric");
				}
				// Pop field owner context
				Value owner = frame.pop();
				if (owner.isNull()) {
					frame.markWonky("putfield 'owner' on stack is null!");
				} else if (!owner.isObject()) {
					frame.markWonky("putfield 'owner' on stack not an object type");
				}
				break;
			}
		}
	}
}
