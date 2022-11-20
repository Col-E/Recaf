package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * Executor for loading values from an array on the stack to the stack.
 *
 * @author Matt Coley
 */
public class ArrayLoadExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		int op = instruction.getOpcodeVal();
		Type fallback = Types.fromArrayOpcode(op);
		Value index = frame.pop();
		Value array = frame.pop();
		if (array instanceof Value.ArrayValue && index instanceof Value.NumericValue) {
			Value.ArrayValue arrayValue = (Value.ArrayValue) array;
			Type elementType = arrayValue.getElementType();
			switch (op) {
				case IALOAD:
					if (elementType != INT_TYPE)
						frame.markWonky("'iaload' used on non int[] array");
					break;
				case LALOAD:
					if (elementType != LONG_TYPE)
						frame.markWonky("'laload' used on non long[] array");
					break;
				case FALOAD:
					if (elementType != FLOAT_TYPE)
						frame.markWonky("'faload' used on non float[] array");
					break;
				case DALOAD:
					if (elementType != DOUBLE_TYPE)
						frame.markWonky("'daload' used on non double[] array");
					break;
				case AALOAD:
					if (elementType.getSort() < ARRAY)
						frame.markWonky("'aaload' used on non Object[] array");
					break;
				case BALOAD:
					if (elementType != BYTE_TYPE)
						frame.markWonky("'iaload' used on non byte[] array");
					break;
				case CALOAD:
					if (elementType != CHAR_TYPE)
						frame.markWonky("'caload' used on non char[] array");
					break;
				case SALOAD:
					if (elementType != SHORT_TYPE)
						frame.markWonky("'saload' used on non short[] array");
					break;
			}
			// Check if we were able to track array size beforehand
			Value[] backingArray = arrayValue.getArray();
			if (backingArray != null) {
				// Check if we know the actual index
				Value.NumericValue arrayIndex = (Value.NumericValue) index;
				if (arrayIndex.getNumber() != null) {
					// Push the real value if possible
					//  - remove fallback so that we don't double-push
					int idx = arrayIndex.getNumber().intValue();
					if (idx >= 0 && idx < backingArray.length) {
						Value value = backingArray[idx];
						if (value != null) {
							frame.push(value);
							// Check for wide types
							if (Types.isWide(fallback))
								frame.push(new Value.WideReservedValue());
							// Reset fallback
							fallback = null;
						}
					}
				}
			}
		} else {
			// Wrong stack value types
			if (array instanceof Value.ArrayValue) {
				frame.markWonky("cannot use index for array operation, index on stack is a non-numeric value");
			} else {
				frame.markWonky("cannot use array for array operation, array on stack is a non-array value");
			}
		}
		// If the fallback hasn't been unset, push it to the stack
		if (fallback != null) {
			if (fallback.getSort() <= Type.FLOAT) {
				frame.push(new Value.NumericValue(fallback));
			} else if (fallback.getSort() <= Type.DOUBLE) {
				frame.push(new Value.NumericValue(fallback));
				frame.push(new Value.WideReservedValue());
			} else {
				frame.push(new Value.ObjectValue(fallback));
			}
		}
	}
}
