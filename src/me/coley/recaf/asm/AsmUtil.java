package me.coley.recaf.asm;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.Recaf;
import me.coley.recaf.util.StreamUtil;

public class AsmUtil {
	private final Recaf recaf = Recaf.getInstance();

	/**
	 * Reads the classes of the given jar into a map.
	 *
	 * @param jarPath Path to jarfile to read classes from.
	 * @return Map of classes from the specified jarfile.
	 * @throws IOException If an error was encountered while reading the
	 * jarfile.
	 */
	public Map<String, ClassNode> readClasses(String jarPath) throws IOException {
		Map<String, ClassNode> map = new HashMap<>();
		try (ZipFile file = new ZipFile(jarPath)) {
			Enumeration<? extends ZipEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
					continue;
				}
				String name = null;
				try (InputStream is = file.getInputStream(entry)) {
					ClassReader cr = new PluginClassReader(is);
					name = cr.getClassName();
					map.put(cr.getClassName(), getNode(cr));
				} catch (IndexOutOfBoundsException ioobe) {
					if (name == null) {
						recaf.window.displayError(new RuntimeException("Failed reading class from: " + entry.getName(),
												  ioobe));
					} else {
						recaf.window.displayError(new RuntimeException("Failed reading into node structure: " + name, ioobe));
					}
				}
			}
		}
		return map;
	}

	/**
	 * Reads non-classes from the given jar.
	 *
	 * @param jarPath Path to jarfile to read non-classes from.
	 * @return Map of non-classes from the specified jarfile.
	 * @throws IOException If an error was encountered while reading the
	 * jarfile.
	 */
	public Map<String, byte[]> readNonClasses(String jarPath) throws IOException {
		Map<String, byte[]> map = new HashMap<>();
		try (ZipFile file = new ZipFile(jarPath)) {
			Enumeration<? extends ZipEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory() || entry.getName().contains(".class")) {
					continue;
				}
				try (InputStream is = file.getInputStream(entry)) {
					map.put(entry.getName(), StreamUtil.fromStream(is));
				}
			}
		}
		return map;
	}

	/**
	 * Creates a ClassNode from the given ClassReader.
	 *
	 * @param cr The class read to obtain the node from.
	 * @return The node obtained from cr.
	 */
	private ClassNode getNode(ClassReader cr) {
		ClassNode cn = new ClassNode();
		cr.accept(cn, recaf.options.classFlagsInput);
		return cn;
	}

	/**
	 * Creates a ClassNode from the given class.
	 *
	 * @param c The target class.
	 * @return Node generated from c.
	 * @throws IOException If an error occurs while loading the class.
	 */
	public ClassNode getNode(Class<?> c) throws IOException {
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
	 * @param cn The target class node.
	 * @return ByteArray representation of cn.
	 */
	public byte[] toBytes(ClassNode cn) {
		ClassWriter cw = new NonReflectionWriter(recaf.options.classFlagsOutput);
		cn.accept(cw);
		return cw.toByteArray();
	}
}
