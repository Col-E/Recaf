package me.coley.recaf.asm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.Program;

public class JarData {
	private final Program callback;
	public final Map<String, ClassNode> classes;
	public final Map<String, byte[]> resources;

	public JarData(Program callback, File jar) throws IOException {
		this.callback = callback;
		String path = jar.getAbsolutePath();
		classes = callback.asm.readClasses(path);
		resources = callback.asm.readNonClasses(path);
	}

	public void save(File jar) throws IOException {
		try (JarOutputStream output = new JarOutputStream(new FileOutputStream(jar))) {
			// write classes
			for (String name : classes.keySet()) {
				ClassWriter cw = new NonReflectionWriter(callback);
				classes.get(name).accept(cw);
				output.putNextEntry(new JarEntry(name.replace(".", "/") + ".class"));
				output.write(cw.toByteArray());
				output.closeEntry();
			}
			// write resources
			for (String name : resources.keySet()) {
				output.putNextEntry(new JarEntry(name));
				output.write(resources.get(name));
				output.closeEntry();
			}
		}
	}
}
