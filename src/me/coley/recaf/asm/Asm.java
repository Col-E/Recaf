package me.coley.recaf.asm;

import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;

import me.coley.recaf.Recaf;
import me.coley.recaf.agent.Agent;
import me.coley.recaf.asm.tracking.TClass;
import me.coley.recaf.util.Streams;

/**
 * Utility for handling a variety of ASM duties <i>(Bytecode loading, parsing,
 * exporting)</i>.
 * 
 * @author Matt
 */
public class Asm {
	/**
	 * Reads the given class into a single-element map.
	 *
	 * @param classPath
	 *            Path to class-file.
	 * @return Map containing the class name to its node instance.
	 * @throws IOException
	 *             If an exception was encountered while reading the class.
	 */
	public static Map<String, ClassNode> readClass(String classPath) {
		String name = null;
		try (InputStream is = new FileInputStream(new File(classPath))) {
			ClassReader cr = new ClassReader(is);
			name = cr.getClassName();
			Map<String, ClassNode> map = new HashMap<>();
			map.put(cr.getClassName(), getNode(cr));
			return map;
		} catch (IndexOutOfBoundsException e) {
			Recaf.INSTANCE.logging.error(new RuntimeException("Failed reading into node structure: " + name, e));
		} catch (IOException e) {
			Recaf.INSTANCE.logging.error(new RuntimeException("Failed reading class from: " + classPath, e));
		}
		return Collections.emptyMap();
	}

	/**
	 * Reads the classes of the given jar into a map.
	 *
	 * @param jarPath
	 *            Path to jarfile to read classes from.
	 * @return Map of classes from the given jarfile.
	 * @throws IOException
	 *             If an exception was encountered while reading the jarfile.
	 */
	public static Map<String, ClassNode> readClasses(String jarPath) throws IOException {
		Map<String, ClassNode> map = new HashMap<>();
		try (ZipFile file = new ZipFile(jarPath)) {
			Enumeration<? extends ZipEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
					continue;
				}
				String name = null;
				Recaf.INSTANCE.logging.fine("Reading jar class: " + entry.getName(), 1);
				try (InputStream is = file.getInputStream(entry)) {
					ClassReader cr = new ClassReader(is);
					name = cr.getClassName();
					map.put(cr.getClassName(), getNode(cr));
				} catch (IndexOutOfBoundsException ioobe) {
					if (name == null) {
						Recaf.INSTANCE.logging.error(new RuntimeException("Failed reading class from: " + entry.getName(),
								ioobe));
					} else {
						Recaf.INSTANCE.logging.error(new RuntimeException("Failed reading into node structure: " + name, ioobe));
					}
				}
			}
		}
		return map;
	}

	/**
	 * Reads non-classes from the given jar.
	 *
	 * @param jarPath
	 *            Path to jarfile to read non-classes from.
	 * @return Map of non-classes from the specified jarfile.
	 * @throws IOException
	 *             If an exception was encountered while reading the jarfile.
	 */
	public static Map<String, byte[]> readNonClasses(String jarPath) throws IOException {
		Map<String, byte[]> map = new HashMap<>();
		try (ZipFile file = new ZipFile(jarPath)) {
			Enumeration<? extends ZipEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory() || entry.getName().contains(".class")) {
					continue;
				}
				Recaf.INSTANCE.logging.fine("Reading jar resource: " + entry.getName(), 1);
				try (InputStream is = file.getInputStream(entry)) {
					map.put(entry.getName(), Streams.from(is));
				}
			}
		}
		return map;
	}

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
	 * Creates a ClassNode from the given ClassReader.
	 *
	 * @param cr
	 *            The ClassReader to obtain the node from.
	 * @return The node obtained from cr.
	 */
	private static ClassNode getNode(ClassReader cr) {
		ClassNode cn = Agent.active() ? new TClass() : new ClassNode();
		cr.accept(cn, Recaf.INSTANCE.configs.asm.classFlagsInput);
		return cn;
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
	 * Writes a ClassNode to a byte array.
	 *
	 * @param cn
	 *            The target ClassNode.
	 * @return ByteArray representation of cn.
	 */
	public static byte[] toBytes(ClassNode cn) {
		ClassWriter cw = new NonReflectionWriter(Recaf.INSTANCE.configs.asm.classFlagsOutput);
		cn.accept(cw);
		return cw.toByteArray();
	}

	/**
	 * Moves the insns up one in the list.
	 * 
	 * @param list
	 *            Complete list of opcodes.
	 * @param insn
	 *            Sublist to be moved.
	 */
	public static void moveUp(InsnList list, List<AbstractInsnNode> insns) {
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
	public static void moveDown(InsnList list, List<AbstractInsnNode> insns) {
		AbstractInsnNode prev = insns.get(insns.size() - 1).getNext();
		if (prev == null) return;
		InsnList x = new InsnList();
		for (AbstractInsnNode ain : insns) {
			list.remove(ain);
			x.add(ain);
		}
		list.insert(prev, x);
	}
}