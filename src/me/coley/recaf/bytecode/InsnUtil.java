package me.coley.recaf.bytecode;

import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.insn.NamedLabelNode;
import me.coley.recaf.util.Reflect;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Objectweb ASM utilities for the AbstractInsnNode tree family.
 *
 * @author Matt
 */
public class InsnUtil {
	/**
	 * Moves the insns up one in the list.
	 *
	 * @param list
	 *            Complete list of opcodes.
	 * @param insns
	 *            Sublist to be moved.
	 */
	public static void shiftUp(InsnList list, List<AbstractInsnNode> insns) {
		if (insns.isEmpty()) {
			return;
		}
		AbstractInsnNode prev = insns.get(0).getPrevious();
		if (prev == null) return;
		InsnList x = new InsnList();
		for (AbstractInsnNode ain : insns) {
			list.remove(ain);
			x.add(ain);
		}
		list.insertBefore(prev, x);
	}

	/**
	 * Moves the insns down one in the list.
	 *
	 * @param list
	 *            Complete list of opcodes.
	 * @param insns
	 *            Sublist to be moved.
	 */
	public static void shiftDown(InsnList list, List<AbstractInsnNode> insns) {
		if (insns.isEmpty()) {
			return;
		}
		AbstractInsnNode prev = insns.get(insns.size() - 1).getNext();
		if (prev == null) return;
		InsnList x = new InsnList();
		for (AbstractInsnNode ain : insns) {
			list.remove(ain);
			x.add(ain);
		}
		list.insert(prev, x);
	}

	/**
	 * Get variable node from method.
	 *
	 * @param method
	 *            Method with local variables.
	 * @param var
	 *            Local variable index.
	 * @return Variable node.
	 */
	public static LocalVariableNode getLocal(MethodNode method, int var) {
		if (method == null || method.localVariables == null) {
			return null;
		}
		// The local variable list cannot be trusted to be in-order.
		for (LocalVariableNode lvn : method.localVariables) {
			if (var == lvn.index) {
				return lvn;
			}
		}
		return null;
	}

	/**
	 * @param opcode
	 *            INSN type opcode.
	 * @return value of INSN type opcode. -2 if value could not be specified.
	 */
	public static int getValue(int opcode) {
		switch (opcode) {
		case Opcodes.ICONST_M1:
			return -1;
		case Opcodes.FCONST_0:
		case Opcodes.LCONST_0:
		case Opcodes.DCONST_0:
		case Opcodes.ICONST_0:
			return 0;
		case Opcodes.FCONST_1:
		case Opcodes.LCONST_1:
		case Opcodes.DCONST_1:
		case Opcodes.ICONST_1:
			return 1;
		case Opcodes.FCONST_2:
		case Opcodes.ICONST_2:
			return 2;
		case Opcodes.ICONST_3:
			return 3;
		case Opcodes.ICONST_4:
			return 4;
		case Opcodes.ICONST_5:
			return 5;
		}
		throw new IllegalStateException("Invalid opcode, does not have a known value: " + opcode);
	}

	/**
	 * Calculate the index of an opcode.
	 *
	 * @param ain
	 *            Opcode.
	 * @return Opcode index.
	 */
	public static int index(AbstractInsnNode ain) {
		return index(ain, null);
	}

	/**
	 * Calculate the index of an opcode in the given method.
	 *
	 * @param ain
	 *            Opcode.
	 * @param method
	 *            Method containing the opcode.
	 * @return Opcode index.
	 */
	public static int index(AbstractInsnNode ain, MethodNode method) {
		// method should cache the index
		if (method != null) {
			return method.instructions.indexOf(ain);
		}
		// calculate manually
		int index = 0;
		while (ain.getPrevious() != null) {
			ain = ain.getPrevious();
			index++;
		}
		return index;
	}

	/**
	 * Size of a method given an instruction in the method.
	 *
	 * @param ain
	 *            The opcode.
	 * @return {@code -1} if the given instruction is null.
	 */
	public static int getSize(AbstractInsnNode ain) {
		if (ain == null)
			return -1;
		int size = index(ain);
		while (ain.getNext() != null) {
			ain = ain.getNext();
			size++;
		}
		return size;
	}

	/**
	 * Number of matching types at and before the given opcode.
	 *
	 * @param type
	 *            Type to check for.
	 * @param ain
	 *            Start index to count backwards from.
	 * @return Number of matching types.
	 */
	public static int count(int type, AbstractInsnNode ain) {
		int count = type == ain.getType() ? 1 : 0;
		if (ain.getPrevious() != null) {
			count += count(type, ain.getPrevious());
		}
		return count;
	}

	/**
	 * @param ain
	 *            Label opcode.
	 * @return Generated label name.
	 */
	public static String labelName(AbstractInsnNode ain) {
		if (ain instanceof NamedLabelNode) {
			return ((NamedLabelNode) ain).name;
		}
		return NamedLabelNode.generateName(count(AbstractInsnNode.LABEL, ain) - 1);
	}

	/**
	 * Links two given insns together via their linked list's previous and next
	 * values. Removing individual items in large changes is VERY slow, so while
	 * this may be ugly its worth it.
	 *
	 * @param instructions
	 * @param insnStart
	 * @param insnEnd
	 */
	public static void link(InsnList instructions, AbstractInsnNode insnStart, AbstractInsnNode insnEnd) {
		try {
			boolean first = instructions.getFirst().equals(insnStart);
			Field next = Reflect.getField(AbstractInsnNode.class, "nextInsn", "next");
			Field prev = Reflect.getField(AbstractInsnNode.class, "previousInsn", "prev");
			next.setAccessible(true);
			prev.setAccessible(true);
			if (first) {
				// Update head
				Field listStart = Reflect.getField(InsnList.class, "firstInsn", "first");
				listStart.setAccessible(true);
				listStart.set(instructions, insnEnd.getNext());
				// Remove link to previous sections
				prev.set(insnEnd.getNext(), null);
			} else {
				// insnStart.prev links to insnEnd.next
				next.set(insnStart.getPrevious(), insnEnd.getNext());
				prev.set(insnEnd.getNext(), insnStart.getPrevious());
			}
			// Reset cache
			Field listStart = InsnList.class.getDeclaredField("cache");
			listStart.setAccessible(true);
			listStart.set(instructions, null);
		} catch (Exception e) {
			Logging.error(e, false);
		}
	}

	/**
	 * @param ain
	 *            Opcode to check.
	 * @return Opcode is not linked to any other node.
	 */
	public static boolean isolated(AbstractInsnNode ain) {
		return ain.getNext() == null && ain.getPrevious() == null;
	}
}
