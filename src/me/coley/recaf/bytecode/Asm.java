package me.coley.recaf.bytecode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.config.impl.ConfASM;

/**
 * Objectweb ASM utilities.
 * 
 * @author Matt
 */
public class Asm {

	/**
	 * Creates a ClassNode from the given bytecode array.
	 * 
	 * @param bs
	 *            Array of class bytecode.
	 * @return The node representation.
	 * @throws IOException
	 *             Thrown if the array could not be streamed.
	 */
	public static ClassNode getNode(byte[] bs) throws IOException {
		return getNode(new ClassReader(new ByteArrayInputStream(bs)));
	}

	/**
	 * Creates a ClassNode from the given class.
	 *
	 * @param c
	 *            The target class.
	 * @return Node generated from the given class.
	 * @throws IOException
	 *             If an exception occurs while loading the class.
	 */
	public static ClassNode getNode(Class<?> c) throws IOException {
		String name = c.getName();
		String path = name.replace('.', '/') + ".class";
		ClassLoader loader = c.getClassLoader();
		if (loader == null) {
			loader = ClassLoader.getSystemClassLoader();
		}
		InputStream is = loader.getResourceAsStream(path);
		ClassReader cr = new ClassReader(is);
		return getNode(cr);
	}

	/**
	 * Common-base of public-facing <i>'getNode'</i> calls.
	 * 
	 * @param cr
	 *            Reader to parse class with.
	 * @return ClassNode from reader.
	 */
	private static ClassNode getNode(ClassReader cr) {
		ClassNode cn = new ClassNode();
		cr.accept(cn, ConfASM.instance().getInputFlags());
		return cn;
	}

	/**
	 * Writes a ClassNode to a byte array.
	 *
	 * @param cn
	 *            The target ClassNode.
	 * @return ByteArray representation of cn.
	 * @throws Exception
	 *             Thrown when the node could not be regenerated. There are a
	 *             few potential causes for this:
	 *             <ul>
	 *             <li>Frames could not be generated due to invalid
	 *             bytecode.</li>
	 *             <li>Frames could not be generated due to inclusion of
	 *             outdated opcodes like <i>JSR/RET</i></li>
	 *             </ul>
	 */
	public static byte[] getBytes(ClassNode cn) throws Exception {
		ClassWriter cw = new NodeParentWriter(ConfASM.instance().getOutputFlags());
		cn.accept(cw);
		return cw.toByteArray();
	}

	/**
	 * IndexOf opcode, independent of InsnList.
	 * 
	 * @param ain
	 *            Opcode to check.
	 * @return Index of opcode.
	 */
	public static int index(AbstractInsnNode ain) {
		int i = 0;
		while (ain.getPrevious() != null) {
			ain = ain.getPrevious();
		}
		return i;
	}

	/**
	 * Moves the insns up one in the list.
	 * 
	 * @param list
	 *            Complete list of opcodes.
	 * @param insn
	 *            Sublist to be moved.
	 */
	public static void shiftUp(InsnList list, List<AbstractInsnNode> insns) {
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
	 * @param insn
	 *            Sublist to be moved.
	 */
	public static void shiftDown(InsnList list, List<AbstractInsnNode> insns) {
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
		if (method.localVariables == null) {
			return null;
		}
		LocalVariableNode lvn = method.localVariables.get(var);
		// Why is the variable table not sorted sometimes????
		if (var != lvn.index) {
			for (LocalVariableNode temp : method.localVariables) {
				if (var == temp.index) {
					return temp;
				}
			}
		}
		return lvn;
	}
}
