package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.ARRAY;
import static org.objectweb.asm.Type.INT;

/**
 * Executor for loading values from an array on the stack to the stack.
 *
 * @author Matt Coley
 */
public class ArrayLoadExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		int op = instruction.getOpcodeVal();
		int fallbackDimensions = -1;
		Type fallback = Types.fromArrayOpcode(op);
		Value index = frame.pop();
		Value array = frame.pop();
		if (array instanceof Value.ArrayValue) {
			Value.ArrayValue arrayValue = (Value.ArrayValue) array;
			Type arrayType = arrayValue.getArrayType();
			fallbackDimensions = arrayType.getDimensions() - 1;
			fallback = arrayValue.getArrayType();
			switch (op) {
				case BALOAD:
				case CALOAD:
				case SALOAD:
				case IALOAD:
					if (arrayType.getElementType().getSort() > INT)
						frame.markWonky("'iaload' used on non int[] (or narrower) array");
					break;
				case LALOAD:
					if (arrayType != Types.LONG_ARRAY_TYPE)
						frame.markWonky("'laload' used on non long[] array");
					break;
				case FALOAD:
					if (arrayType != Types.FLOAT_ARRAY_TYPE)
						frame.markWonky("'faload' used on non float[] array");
					break;
				case DALOAD:
					if (arrayType != Types.DOUBLE_ARRAY_TYPE)
						frame.markWonky("'daload' used on non double[] array");
					break;
				case AALOAD:
					if (arrayType.getSort() < ARRAY)
						frame.markWonky("'aaload' used on non Object[] array");
					break;

			}
			// Check if we were able to track array size beforehand
			Value[] backingArray = arrayValue.getArray();
			if (backingArray != null && index.isNumeric()) {
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
			if (!index.isNumeric()) {
				frame.markWonky("cannot use index for array operation, index on stack is a non-numeric value");
			} else if (!array.isArray()) {
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
			} else if (fallback.getSort() == ARRAY) {
				if (fallbackDimensions > 0) {
					// It's an array in an array
					frame.push(new Value.ArrayValue(fallbackDimensions, fallback.getElementType()));
				} else {
					// It's a value in an array
					Type elementType = fallback.getElementType();
					if (elementType.getSort() <= Type.FLOAT) {
						frame.push(new Value.NumericValue(elementType));
					} else if (elementType.getSort() <= Type.DOUBLE) {
						frame.push(new Value.NumericValue(elementType));
						frame.push(new Value.WideReservedValue());
					} else {
						frame.push(new Value.ObjectValue(elementType));
					}
				}
			} else {
				frame.push(new Value.ObjectValue(fallback));
			}
		}
	}
}
