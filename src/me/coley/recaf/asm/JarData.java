package me.coley.recaf.asm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.Recaf;
import me.coley.recaf.agent.Agent;
import me.coley.recaf.event.impl.EAgentOpen;
import me.coley.recaf.event.impl.EFileOpen;
import me.coley.recaf.event.impl.EFileSave;

/**
 * Storage of classes and resources from the currently loaded jar file.
 * 
 * @author Matt
 */
public class JarData {
	/**
	 * The file.
	 */
	public final File jar;
	/**
	 * Map of class names to ClassNode representations of the classes.
	 */
	public final Map<String, ClassNode> classes;
	/**
	 * Map of resource names to their raw bytes.
	 */
	public final Map<String, byte[]> resources;

	/**
	 * Constructor for loading via jar-file.
	 * 
	 * @param inJar
	 *            Jar file to read from.
	 * @throws IOException
	 *             Thrown if jar file could not be read completely.
	 */
	public JarData(File inJar) throws IOException {
		jar = inJar;
		String path = inJar.getAbsolutePath();
		classes = Asm.readClasses(path);
		resources = Asm.readNonClasses(path);
		int c = classes.size(), r = resources.size();
		Recaf.INSTANCE.logging.info("Loaded jar: " + inJar.getName() + " [" + c + " classes, " + r + " resources]");
		Recaf.INSTANCE.bus.post(new EFileOpen(inJar, classes, resources));
	}

	/**
	 * Constructor for loading via java-agent.
	 * 
	 * @throws IOException
	 *             Thrown if classes could not be read from vm.
	 */
	public JarData() throws IOException {
		jar = null;
		classes = Agent.getNodesViaInst();
		resources = Collections.emptyMap();
		int c = classes.size();
		Recaf.INSTANCE.logging.info("Loaded classes: [" + c + " classes]");
		Recaf.INSTANCE.bus.post(new EAgentOpen(classes));
	}

	/**
	 * Saves the classes and resources to the given file.
	 * 
	 * @param outFile
	 *            File name to save contents to.
	 * @throws IOException
	 *             Thrown if the output could not be created or written to.
	 */
	public void save(File outFile) throws IOException {
		// write classes
		Map<String, byte[]> contents = new HashMap<>();
		for (Entry<String, ClassNode> entry : classes.entrySet()) {
			ClassNode cn = entry.getValue();
			byte[] data = Asm.toBytes(cn);
			contents.put(cn.name + ".class", data);
		}
		// write resources
		for (Entry<String, byte[]> entry : resources.entrySet()) {
			contents.put(entry.getKey(), entry.getValue());
		}
		// Post to event bus
		Recaf.INSTANCE.bus.post(new EFileSave(outFile, contents));
		// Save contents
		try (JarOutputStream output = new JarOutputStream(new FileOutputStream(outFile))) {
			for (Entry<String, byte[]> entry : contents.entrySet()) {
				output.putNextEntry(new JarEntry(entry.getKey()));
				output.write(entry.getValue());
				output.closeEntry();
			}
		}
	}
}