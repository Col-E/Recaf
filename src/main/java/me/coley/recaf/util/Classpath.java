package me.coley.recaf.util;

import com.google.common.reflect.ClassPath;

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

	public static Collection<String> getClasspathNames() {
		if(classpathNames == null)
			setupClassPathMaster();
		return classpathNames;
	}
}
