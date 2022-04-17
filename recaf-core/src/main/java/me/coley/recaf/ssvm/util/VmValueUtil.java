package me.coley.recaf.ssvm.util;

import dev.xdark.ssvm.value.*;
import me.coley.recaf.ssvm.value.ConstValue;
import me.coley.recaf.ssvm.value.TrackedArrayValue;
import me.coley.recaf.util.InstructionUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;

/**
 * Utility functions for operating on {@link Value}.
 *
 * @author Matt Coley
 */
public class VmValueUtil implements Opcodes {
	/**
	 * @param value
	 * 		Wrapper of a value that needs to be pushed by an instruction.
	 * 		Must be a {@link NumericValue} or {@link NullValue}.
	 *
	 * @return Single instruction to push the primitive or {@code null} value.
	 */
	public static AbstractInsnNode createConstInsn(Value value) {
		if (value instanceof DelegatingValue)
			return createConstInsn(((DelegatingValue<?>) value).getDelegate());
		else if (value instanceof NumericValue) {
			if (value instanceof IntValue) return InstructionUtil.createIntPush(value.asInt());
			else if (value instanceof LongValue) return InstructionUtil.createLongPush(value.asLong());
			else if (value instanceof FloatValue) return InstructionUtil.createFloatPush(value.asFloat());
			else if (value instanceof DoubleValue) return InstructionUtil.createDoublePush(value.asDouble());
			else throw new IllegalStateException("Unknown numeric value type: " + value.getClass().getName());
		} else if (value == NullValue.INSTANCE)
			return new InsnNode(ACONST_NULL);
		throw new IllegalStateException("Cannot create constant for value: " + value);
	}
}
