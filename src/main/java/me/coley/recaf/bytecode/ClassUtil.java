package me.coley.recaf.bytecode;

import java.io.IOException;
import java.io.InputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import me.coley.recaf.config.impl.ConfASM;

/**
 * Objectweb ASM ClassNode utilities.
 * 
 * @author Matt
 */
public class ClassUtil {

	/**
	 * Creates a ClassNode from the given bytecode array.
	 * 
	 * @param bs
	 *            Array of class bytecode.
	 * @return The node representation.
	 */
	public static ClassNode getNode(byte[] bs) {
		return getNode(new ClassReader(bs));
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
		ClassReader cr = newClassReader(c);
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
	 * Creates a new {@link ClassReader} instance of the specified class.
	 *
	 * @throws IOException if a problem occurs during reading.
	 */
	public static ClassReader newClassReader(Class<?> cls) throws IOException {
		ClassLoader loader = cls.getClassLoader();
		if (loader != null) {
			String path = Type.getInternalName(cls).concat(".class");
			try (InputStream in = loader.getResourceAsStream(path)) {
				return new ClassReader(in);
			}
		} else {
			return new ClassReader(cls.getName());
		}
	}
}
