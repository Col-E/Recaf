package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

/**
 * ASM instruction utilities.
 *
 * @author Matt Coley
 */
public class AsmInsnUtil implements Opcodes {
	/**
	 * @param type
	 * 		Type to push.
	 *
	 * @return Instruction to push a default value of the given type onto the stack.
	 */
	@Nonnull
	public static AbstractInsnNode getDefaultValue(@Nonnull Type type) {
		return switch (type.getSort()) {
			case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> new InsnNode(ICONST_0);
			case Type.LONG -> new InsnNode(LCONST_0);
			case Type.FLOAT -> new InsnNode(FCONST_0);
			case Type.DOUBLE -> new InsnNode(DCONST_0);
			default -> new InsnNode(ACONST_NULL);
		};
	}

	/**
	 * Create an instruction to hold a given {@code int} value.
	 *
	 * @param value
	 * 		Value to hold.
	 *
	 * @return Insn with const value.
	 */
	@Nonnull
	public static AbstractInsnNode intToInsn(int value) {
		switch (value) {
			case -1:
				return new InsnNode(ICONST_M1);
			case 0:
				return new InsnNode(ICONST_0);
			case 1:
				return new InsnNode(ICONST_1);
			case 2:
				return new InsnNode(ICONST_2);
			case 3:
				return new InsnNode(ICONST_3);
			case 4:
				return new InsnNode(ICONST_4);
			case 5:
				return new InsnNode(ICONST_5);
			default:
				if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
					return new IntInsnNode(BIPUSH, value);
				} else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
					return new IntInsnNode(SIPUSH, value);
				} else {
					return new LdcInsnNode(value);
				}
		}
	}

	/**
	 * Create an instruction to hold a given {@code float} value.
	 *
	 * @param value
	 * 		Value to hold.
	 *
	 * @return Insn with const value.
	 */
	@Nonnull
	public static AbstractInsnNode floatToInsn(float value) {
		if (value == 0)
			return new InsnNode(FCONST_0);
		if (value == 1)
			return new InsnNode(FCONST_1);
		if (value == 2)
			return new InsnNode(FCONST_2);
		return new LdcInsnNode(value);
	}

	/**
	 * Create an instruction to hold a given {@code double} value.
	 *
	 * @param value
	 * 		Value to hold.
	 *
	 * @return Insn with const value.
	 */
	@Nonnull
	public static AbstractInsnNode doubleToInsn(double value) {
		if (value == 0)
			return new InsnNode(DCONST_0);
		if (value == 1)
			return new InsnNode(DCONST_1);
		return new LdcInsnNode(value);
	}

	/**
	 * Create an instruction to hold a given {@code long} value.
	 *
	 * @param value
	 * 		Value to hold.
	 *
	 * @return Insn with const value.
	 */
	@Nonnull
	public static AbstractInsnNode longToInsn(long value) {
		if (value == 0)
			return new InsnNode(LCONST_0);
		if (value == 1)
			return new InsnNode(LCONST_1);
		return new LdcInsnNode(value);
	}

	/**
	 * @param type
	 * 		Some type.
	 *
	 * @return Method return instruction opcode for the given type.
	 */
	public static int getReturnOpcode(@Nonnull Type type) {
		return switch (type.getSort()) {
			case Type.VOID -> RETURN;
			case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> IRETURN;
			case Type.FLOAT -> FRETURN;
			case Type.LONG -> LRETURN;
			case Type.DOUBLE -> DRETURN;
			default -> ARETURN;
		};
	}
}
