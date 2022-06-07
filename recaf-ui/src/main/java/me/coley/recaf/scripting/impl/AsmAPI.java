package me.coley.recaf.scripting.impl;

import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.util.visitor.WorkspaceClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

/**
 * Utility functions for working with ow2-asm.
 *
 * @author Matt Coley
 */
public class AsmAPI {
	/**
	 * @param info
	 * 		Class info wrapper.
	 * @param readFlags
	 *        {@link ClassReader} flags.
	 *
	 * @return ASM node representation of class.
	 */
	public static ClassNode toNode(ClassInfo info, int readFlags) {
		return toNode(info.getValue(), readFlags);
	}

	/**
	 * @param code
	 * 		Class bytecode.
	 * @param readFlags
	 *        {@link ClassReader} flags.
	 *
	 * @return ASM node representation of class.
	 */
	public static ClassNode toNode(byte[] code, int readFlags) {
		return toNode(new ClassReader(code), readFlags);
	}

	/**
	 * @param reader
	 * 		ASM class reader containing class bytecode.
	 * @param readFlags
	 *        {@link ClassReader} flags.
	 *
	 * @return ASM node representation of class.
	 */
	public static ClassNode toNode(ClassReader reader, int readFlags) {
		ClassNode node = new ClassNode();
		reader.accept(node, readFlags);
		return node;
	}

	/**
	 * @param node
	 * 		ASM node representation of class.
	 * @param writeFlags
	 *        {@link ClassWriter} flags. Should almost always be {@link ClassWriter#COMPUTE_FRAMES}.
	 *
	 * @return Class bytecode.
	 */
	public static byte[] fromNode(ClassNode node, int writeFlags) {
		ClassWriter writer = new WorkspaceClassWriter(RecafUI.getController(), writeFlags);
		node.accept(writer);
		return writer.toByteArray();
	}
}
