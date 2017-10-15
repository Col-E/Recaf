package me.coley.recaf.asm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.Recaf;

public class JarData {
	private final Recaf recaf = Recaf.INSTANCE;
	public final Map<String, ClassNode> classes;
	public final Map<String, byte[]> resources;

	public JarData(File jar) throws IOException {
		String path = jar.getAbsolutePath();
		classes = recaf.asm.readClasses(path);
		resources = recaf.asm.readNonClasses(path);
	}

	public void save(File jar) throws IOException {
		try (JarOutputStream output = new JarOutputStream(new FileOutputStream(jar))) {
			// write classes
			for (Entry<String, ClassNode> entry : classes.entrySet()) {
				byte[] data = recaf.asm.toBytes(entry.getValue());
				output.putNextEntry(new JarEntry(entry.getKey().replace(".", "/") + ".class"));
				output.write(data);
				output.closeEntry();
			}
			// write resources
			for (Entry<String, byte[]> entry : resources.entrySet()) {
				output.putNextEntry(new JarEntry(entry.getKey()));
				output.write(entry.getValue());
				output.closeEntry();
			}
		}
	}
}
