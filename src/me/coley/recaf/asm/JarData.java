package me.coley.recaf.asm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class JarData {
	public Map<String, ClassNode> classes = new HashMap<>();
	public Map<String, byte[]> resources = new HashMap<>();

	public JarData(File jar) throws IOException {
		String path = jar.getAbsolutePath();
		classes = AsmUtil.readClasses(path);
		resources = AsmUtil.readNonClasses(path);
	}

	public void save(File jar) throws IOException {
		try (JarOutputStream output = new JarOutputStream(new FileOutputStream(jar))) {
			// write classes
			for (String name : classes.keySet()) {
				ClassWriter cw = new NonReflectionWriter(classes);
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
