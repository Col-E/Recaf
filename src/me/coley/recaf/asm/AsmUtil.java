package me.coley.recaf.asm;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.util.StreamUtil;

public class AsmUtil {
	/**
	 * Reads the classes of the given jar into a map.
	 * 
	 * @param jarPath
	 * @return
	 * @throws IOException
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
				try (InputStream is = file.getInputStream(entry)) {
					ClassReader cr = new ClassReader(is);
					map.put(cr.getClassName(), getNode(cr));
				}
			}
		}
		return map;
	}

	/**
	 * Reads non-classes from the given jar.
	 * 
	 * @param jarPath
	 * @return
	 * @throws IOException
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
	 * @param cr
	 * @return
	 */
	public static ClassNode getNode(ClassReader cr) {
		ClassNode cn = new ClassNode();
		try {
			cr.accept(cn, ClassReader.EXPAND_FRAMES);
		} catch (Exception e) {}
		return cn;
	}

	/**
	 * Creates a ClassNode from the given class.
	 * 
	 * @param c
	 * @return
	 * @throws IOException
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
}
