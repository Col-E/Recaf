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

import me.coley.recaf.Program;
import me.coley.recaf.util.StreamUtil;

public class AsmUtil {
	private final Program callback = Program.getInstance();

	/**
	 * Reads the classes of the given jar into a map.
	 * 
	 * @param jarPath
	 * @return
	 * @throws IOException
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
						callback.window.displayError(new RuntimeException("Failed reading class from: " + entry.getName(),
								ioobe));
					} else {
						callback.window.displayError(new RuntimeException("Failed reading into node structure: " + name, ioobe));
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
	 * @return
	 * @throws IOException
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
	 * @param cr
	 * @return
	 */
	private ClassNode getNode(ClassReader cr) {
		ClassNode cn = new ClassNode();
		cr.accept(cn, callback.options.classFlagsInput);
		return cn;
	}

	/**
	 * Creates a ClassNode from the given class.
	 * 
	 * @param c
	 * @return
	 * @throws IOException
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
}
