package me.coley.recaf.util;

import com.google.common.reflect.ClassPath;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class Classpath {
	/**
	 * System classloader
	 */
	private final static ClassLoader scl = ClassLoader.getSystemClassLoader();
	/**
	 * Classpath class names.
	 */
	private static Collection<String> classpathNames;

	/**
	 * Setup {@link #classpathNames}.
	 */
	private static void setupClassPathMaster() {
		try {
			classpathNames = ClassPath.from(scl).getAllClasses().stream()
					.map(info -> info.getName().replace(".", "/"))
					.collect(Collectors.toList());
		} catch(Exception e) {
			classpathNames = new ArrayList<>();
		}
	}

	/**
	 * @return Names of classes in the class-path.
	 */
	public static Collection<String> getClasspathNames() {
		if(classpathNames == null)
			setupClassPathMaster();
		return classpathNames;
	}

	/**
	 * @param clazz
	 * 		Class to get bytecode of.
	 *
	 * @return Class's bytecode.
	 *
	 * @throws IOException
	 * 		thrown if the class couldn't be fetched as a stream.
	 */
	public static byte[] getClass(Class<?> clazz) throws IOException {
		String name = clazz.getName();
		String path = name.replace('.', '/') + ".class";
		ClassLoader loader = clazz.getClassLoader();
		if (loader == null) {
			loader = ClassLoader.getSystemClassLoader();
		}
		InputStream is = loader.getResourceAsStream(path);
		return Streams.from(is);
	}
}
