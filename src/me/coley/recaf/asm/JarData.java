package me.coley.recaf.asm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.Recaf;
import me.coley.recaf.event.impl.EFileOpen;
import me.coley.recaf.event.impl.EFileSave;

/**
 * Storage of classes and resources from the currently loaded jar file.
 * 
 * @author Matt
 */
public class JarData {
	private final Recaf recaf = Recaf.INSTANCE;
	/**
	 * Map of class names to ClassNode representations of the classes.
	 */
	public final Map<String, ClassNode> classes;
	/**
	 * Map of resource names to their raw bytes.
	 */
	public final Map<String, byte[]> resources;

	/**
	 * @param jar
	 *            Jar file to read from.
	 * @throws IOException
	 *             Thrown if jar file could not be read completely.
	 */
	public JarData(File jar) throws IOException {
		String path = jar.getAbsolutePath();
		classes = recaf.asm.readClasses(path);
		resources = recaf.asm.readNonClasses(path);
		recaf.bus.post(new EFileOpen(jar, classes,resources));
	}

	/**
	 * Saves the classes and resources to the given file.
	 * 
	 * @param jar
	 *            File name to save contents to.
	 * @throws IOException
	 *             Thrown if the output could not be created or written to.
	 */
	public void save(File jar) throws IOException {
		// write classes
		Map<String, byte[]> contents = new HashMap<>();
		for (Entry<String, ClassNode> entry : classes.entrySet()) {
			ClassNode cn = entry.getValue();
			byte[] data = recaf.asm.toBytes(cn);
			contents.put(cn.name + ".class", data);
		}
		// write resources
		for (Entry<String, byte[]> entry : resources.entrySet()) {
			contents.put(entry.getKey(), entry.getValue());
		}
		// Post to event bus
		recaf.bus.post(new EFileSave(jar, contents));
		// Save contents
		try (JarOutputStream output = new JarOutputStream(new FileOutputStream(jar))) {
			for (Entry<String, byte[]> entry : contents.entrySet()) {
				output.putNextEntry(new JarEntry(entry.getKey()));
				output.write(entry.getValue());
				output.closeEntry();
			}
		}
	}
}
