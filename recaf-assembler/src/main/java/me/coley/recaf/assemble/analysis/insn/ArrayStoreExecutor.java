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
 * Executor for storing stack values into arrays <i>(also on the stack)</i>.
 *
 * @author Matt Coley
 */
public class ArrayStoreExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		int op = instruction.getOpcodeVal();
		Type type = Types.fromArrayOpcode(op);
		// Stack order (top-bottom): value, index, arrayref
		Value value = Types.isWide(type) ? frame.popWide() : frame.pop();
		Value index = frame.pop();
		Value array = frame.pop();
		if (array instanceof Value.ArrayValue && index instanceof Value.NumericValue) {
			Value.ArrayValue arrayValue = (Value.ArrayValue) array;
			Type arrayType = arrayValue.getArrayType();
			switch (op) {
				case IASTORE:
					if (arrayType.getElementType().getSort() > INT)
						frame.markWonky("'iastore' used on non int[] (or narrower type) array");
					break;
				case LASTORE:
					if (arrayType != Types.LONG_ARRAY_TYPE)
						frame.markWonky("'lastore' used on non long[] array");
					break;
				case FASTORE:
					if (arrayType != Types.FLOAT_ARRAY_TYPE)
						frame.markWonky("'fastore' used on non float[] array");
					break;
				case DASTORE:
					if (arrayType != Types.DOUBLE_ARRAY_TYPE)
						frame.markWonky("'dastore' used on non double[] array");
					break;
				case AASTORE:
					if (arrayType.getSort() < ARRAY)
						frame.markWonky("'aastore' used on non Object[] array");
					break;
				case BASTORE:
					if (arrayType != BYTE_TYPE)
						frame.markWonky("'iastore' used on non byte[] array");
					break;
				case CASTORE:
					if (arrayType != CHAR_TYPE)
						frame.markWonky("'castore' used on non char[] array");
					break;
				case SASTORE:
					if (arrayType != SHORT_TYPE)
						frame.markWonky("'sastore' used on non short[] array");
					break;
			}
			// Check if we were able to track array size beforehand
			Value[] backingArray = arrayValue.getArray();
			if (backingArray != null) {
				// Check if we know the actual index
				Value.NumericValue arrayIndex = (Value.NumericValue) index;
				if (arrayIndex.getNumber() != null) {
					// Update the array
					int idx = arrayIndex.getNumber().intValue();
					if (idx >= 0 && idx < backingArray.length)
						backingArray[idx] = value;
					else
						// Should not occur
						frame.markWonky("cannot store index in array '" + idx + "' because it is out of bounds");
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
	}
}
