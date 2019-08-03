package me.coley.recaf.util;

import static org.objectweb.asm.Opcodes.*;

/**
 * Instruction level utilities.
 *
 * @author Matt
 */
public class InsnUtil {
	/**
	 * @param opcode
	 * 		Instruction opcode. Should be of type
	 * 		{@link org.objectweb.asm.tree.AbstractInsnNode#INSN}.
	 *
	 * @return value represented by the instruction.
	 *
	 * @throws IllegalArgumentException
	 * 		Thrown if the opcode does not have a known value.
	 */
	public static int getValue(int opcode) {
		switch(opcode) {
			case ICONST_M1:
				return -1;
			case FCONST_0:
			case LCONST_0:
			case DCONST_0:
			case ICONST_0:
				return 0;
			case FCONST_1:
			case LCONST_1:
			case DCONST_1:
			case ICONST_1:
				return 1;
			case FCONST_2:
			case ICONST_2:
				return 2;
			case ICONST_3:
				return 3;
			case ICONST_4:
				return 4;
			case ICONST_5:
				return 5;
			default:
				throw new IllegalArgumentException("Invalid opcode, does not have a known value: " + opcode);
		}
	}
}
