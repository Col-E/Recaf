package me.coley.recaf.util;

import org.objectweb.asm.ClassReader;

import java.io.IOException;

/**
 * Utilities for dealing with class-file loading/parsing.
 *
 * @author Matt
 */
public class ClassUtil {
	/**
	 * @param name
	 * 		Internal class name.
	 *
	 * @return {@link org.objectweb.asm.ClassReader} loaded from runtime.
	 */
	public static ClassReader fromRuntime(String name) {
		try {
			Class<?> loaded = ClasspathUtil.getSystemClass(normalize(name));
			return new ClassReader(loaded.getName());
		} catch(ClassNotFoundException | IOException e) {
			// Expected / allowed: ignore these
		} catch(Exception ex) {
			// Unexpected
			throw new IllegalStateException("Failed to load class from runtime: " + name, ex);
		}
		return null;
	}

	/**
	 * @param internal
	 * 		Internal class name.
	 *
	 * @return Standard class name.
	 */
	private static String normalize(String internal) {
		return internal.replace("/", ".").replace("$", ".");
	}
}
