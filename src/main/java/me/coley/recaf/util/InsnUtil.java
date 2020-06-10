package me.coley.recaf.util;

import org.objectweb.asm.tree.AbstractInsnNode;

import java.lang.reflect.Field;

import static org.objectweb.asm.Opcodes.*;

/**
 * Instruction level utilities.
 *
 * @author Matt
 */
public class InsnUtil {
	private static Field INSN_INDEX;

	/**
	 * @param opcode
	 * 		Instruction opcode. Should be of type
	 *        {@link org.objectweb.asm.tree.AbstractInsnNode#INSN}.
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

	/**
	 * Calculate the index of an instruction.
	 *
	 * @param ain
	 * 		instruction.
	 *
	 * @return Instruction index.
	 */
	public static int index(AbstractInsnNode ain) {
		try {
			int v = (int) INSN_INDEX.get(ain);
			// Can return -1
			if (v >= 0)
				return v;
		} catch(Exception ex) { /* Fail */ }
		// Fallback
		int index = 0;
		while(ain.getPrevious() != null) {
			ain = ain.getPrevious();
			index++;
		}
		return index;
	}

	/**
	 * Get the first insn connected to the given one.
	 *
	 * @param insn
	 * 		instruction
	 *
	 * @return First insn in the insn-list.
	 */
	public static AbstractInsnNode getFirst(AbstractInsnNode insn) {
		while(insn.getPrevious() != null)
			insn = insn.getPrevious();
		return insn;
	}

	static {
		try {
			INSN_INDEX = AbstractInsnNode.class.getDeclaredField("index");
			INSN_INDEX.setAccessible(true);
		} catch(Exception ex) {
			Log.warn("Failed to fetch AbstractInsnNode index field!");
		}
	}
}
