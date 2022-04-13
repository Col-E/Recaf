package me.coley.recaf.ssvm.loader;

import dev.xdark.ssvm.classloading.BootClassLoader;
import dev.xdark.ssvm.classloading.ClassParseResult;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;

/**
 * Loader that pulls classes from the current runtime.
 *
 * @author Matt Coley
 */
public class RuntimeBootClassLoader implements BootClassLoader {
	@Override
	public ClassParseResult findBootClass(String name) {
		try {
			ClassReader reader = new ClassReader(name);
			ClassNode node = new ClassNode();
			reader.accept(node, ClassReader.SKIP_FRAMES);
			return new ClassParseResult(reader, node);
		} catch (IOException ex) {
			// Return null if not found
			return null;
		}
	}
}
