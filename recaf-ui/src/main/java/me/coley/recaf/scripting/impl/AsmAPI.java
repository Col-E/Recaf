package me.coley.recaf.scripting.impl;

import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.util.visitor.WorkspaceClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

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

	/**
	 * @param node
	 * 		Class to search in.
	 * @param name
	 * 		Field name, or {@code null} to ignore name matching.
	 * @param desc
	 * 		Field descriptor, or {@code null} to ignore descriptor matching.
	 *
	 * @return First field matching the given name/desc.
	 */
	public static FieldNode findField(ClassNode node, String name, String desc) {
		for (FieldNode field : node.fields) {
			if ((name == null || field.name.equals(name)) && (desc == null || field.desc.equals(desc)))
				return field;
		}
		return null;
	}

	/**
	 * @param node
	 * 		Class to search in.
	 * @param name
	 * 		Field name, or {@code null} to ignore name matching.
	 * @param desc
	 * 		Field descriptor, or {@code null} to ignore descriptor matching.
	 *
	 * @return All fields matching the given name/desc.
	 */
	public static List<FieldNode> findFields(ClassNode node, String name, String desc) {
		List<FieldNode> list = new ArrayList<>();
		for (FieldNode field : node.fields) {
			if ((name == null || field.name.equals(name)) && (desc == null || field.desc.equals(desc)))
				list.add(field);
		}
		return list;
	}

	/**
	 * @param node
	 * 		Class to search in.
	 *
	 * @return The static initializer method, or {@code null} if there is none in the class.
	 */
	public static MethodNode findStaticInitializer(ClassNode node) {
		return findMethod(node, "<clinit>", "()V");
	}

	/**
	 * @param node
	 * 		Class to search in.
	 * @param desc
	 * 		Descriptor of constructor, or {@code null} to ignore descriptor matching.
	 *
	 * @return The constructor matching the descriptor, or {@code null} for no match.
	 */
	public static MethodNode findConstructor(ClassNode node, String desc) {
		return findMethod(node, "<init>", desc);
	}

	/**
	 * @param node
	 * 		Class to search in.
	 *
	 * @return All constructors in the class.
	 */
	public static List<MethodNode> findConstructors(ClassNode node) {
		return findMethods(node, "<init>", null);
	}

	/**
	 * @param node
	 * 		Class to search in.
	 * @param name
	 * 		Method name, or {@code null} to ignore name matching.
	 * @param desc
	 * 		Method descriptor, or {@code null} to ignore descriptor matching.
	 *
	 * @return First method matching the given name/desc.
	 */
	public static MethodNode findMethod(ClassNode node, String name, String desc) {
		for (MethodNode method : node.methods) {
			if ((name == null || method.name.equals(name)) && (desc == null || method.desc.equals(desc)))
				return method;
		}
		return null;
	}

	/**
	 * @param node
	 * 		Class to search in.
	 * @param name
	 * 		Method name, or {@code null} to ignore name matching.
	 * @param desc
	 * 		Method descriptor, or {@code null} to ignore descriptor matching.
	 *
	 * @return All methods matching the given name/desc.
	 */
	public static List<MethodNode> findMethods(ClassNode node, String name, String desc) {
		List<MethodNode> list = new ArrayList<>();
		for (MethodNode method : node.methods) {
			if ((name == null || method.name.equals(name)) && (desc == null || method.desc.equals(desc)))
				list.add(method);
		}
		return list;
	}
}
