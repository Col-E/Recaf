package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.VarInstruction;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

/**
 * Executor for storing stack values into local variables.
 *
 * @author Matt Coley
 */
public class VarStoreExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		if (!(instruction instanceof VarInstruction))
			throw new AnalysisException(instruction, "Expected variable insn");
		VarInstruction varInstruction = (VarInstruction) instruction;
		int op = varInstruction.getOpcodeVal();
		String varName = varInstruction.getName();
		boolean isWideStore = op == DSTORE || op == LSTORE;
		switch (op) {
			case ISTORE: {
				Value top = frame.peek();
				if (!(top instanceof Value.NumericValue)) {
					frame.markWonky("'istore' used when value to store is not-numeric");
				} else {
					Value.NumericValue numericValue = (Value.NumericValue) top;
					Type type = numericValue.getType();
					if (type.getSort() > Type.INT) {
						frame.markWonky("'istore' used when value to store is not boolean/char/byte/short/int," +
								" got: " + type.getClassName());
					}
				}
				break;
			}
			case LSTORE: {
				Value top = frame.peek();
				Value topM1 = frame.peek(1);
				if (!top.isWideReserved()) {
					frame.markWonky("'lstore' used without wide padding value on the stack");
				} else if (!topM1.isWideNumeric()) {
					frame.markWonky("'lstore' used when value to store is not-numeric");
				} else {
					Value.NumericValue numericValue = (Value.NumericValue) topM1;
					Type type = numericValue.getType();
					if (type != Type.LONG_TYPE) {
						frame.markWonky("'lstore' used when value to store is not 'long', got: " + type.getClassName());
					}
				}
				break;
			}
			case FSTORE: {
				Value top = frame.peek();
				if (!(top instanceof Value.NumericValue)) {
					frame.markWonky("'fstore' used when value to store is not-numeric");
				} else {
					Value.NumericValue numericValue = (Value.NumericValue) top;
					Type type = numericValue.getType();
					if (type != Type.FLOAT_TYPE) {
						frame.markWonky("'fstore' used when value to store is not 'float', got: " + type.getClassName());
					}
				}
				break;
			}
			case DSTORE: {
				Value top = frame.peek();
				Value topM1 = frame.peek(1);
				if (!top.isWideReserved()) {
					frame.markWonky("'dstore' used without wide padding value on the stack");
				} else if (!topM1.isWideNumeric()) {
					frame.markWonky("'dstore' used when value to store is not-numeric");
				} else {
					Value.NumericValue numericValue = (Value.NumericValue) topM1;
					Type type = numericValue.getType();
					if (type != Type.DOUBLE_TYPE) {
						frame.markWonky("'dstore' used when value to store is not 'double', got: " + type.getClassName());
					}
				}
				break;
			}
			case ASTORE: {
				Value top = frame.peek();
				if (!top.isNull() && !top.isObject() && !top.isArray())
					frame.markWonky("'astore' used on non-object value");
				break;
			}
		}
		// Store value into local
		Value value = isWideStore ? frame.popWide() : frame.pop();
		frame.setLocal(varName, value);
	}
}
