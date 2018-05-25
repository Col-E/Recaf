package me.coley.recaf.bytecode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;

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
	 */
	public static byte[] getBytes(ClassNode cn) {
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
}
