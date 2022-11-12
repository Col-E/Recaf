package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.ArgType;
import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.LdcInstruction;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Type.*;

/**
 * Executor for pushing int values onto the stack.
 * <br>
 * Use by {@code XPUSH} instructions.
 *
 * @author Matt Coley
 */
public class LdcExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		if (!(instruction instanceof LdcInstruction))
			throw new AnalysisException(instruction, "Expected ldc insn");
		LdcInstruction ldcInstruction = (LdcInstruction) instruction;
		ArgType valueType = ldcInstruction.getValueType();
		switch (valueType) {
			case TYPE:
				frame.push(new Value.TypeValue((Type) ldcInstruction.getValue()));
				break;
			case STRING:
				frame.push(new Value.StringValue((String) ldcInstruction.getValue()));
				break;
			case BOOLEAN:
				frame.push(new Value.NumericValue(BOOLEAN_TYPE, (Boolean) ldcInstruction.getValue() ? 1 : 0));
				break;
			case BYTE:
				frame.push(new Value.NumericValue(BYTE_TYPE, (Byte) ldcInstruction.getValue()));
				break;
			case SHORT:
				frame.push(new Value.NumericValue(SHORT_TYPE, (Short) ldcInstruction.getValue()));
				break;
			case CHAR:
				frame.push(new Value.NumericValue(CHAR_TYPE, (int) (Character) ldcInstruction.getValue()));
				break;
			case INTEGER:
				frame.push(new Value.NumericValue(INT_TYPE, (Integer) ldcInstruction.getValue()));
				break;
			case FLOAT:
				frame.push(new Value.NumericValue(FLOAT_TYPE, (Float) ldcInstruction.getValue()));
				break;
			case DOUBLE:
				frame.push(new Value.NumericValue(DOUBLE_TYPE, (Double) ldcInstruction.getValue()));
				frame.push(new Value.WideReservedValue());
				break;
			case LONG:
				frame.push(new Value.NumericValue(LONG_TYPE, (Long) ldcInstruction.getValue()));
				frame.push(new Value.WideReservedValue());
				break;
			case HANDLE:
				frame.push(new Value.HandleValue(new HandleInfo((Handle) ldcInstruction.getValue())));
				break;
			default:
				throw new AnalysisException(ldcInstruction, "Unsupported LDC value type: " + valueType);
		}
	}
}
