package me.coley.recaf.util;

import com.google.common.reflect.ClassPath;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Classpath {
	/**
	 * The system classloader, provided by {@link ClassLoader#getSystemClassLoader()}.
	 */
	public static final ClassLoader scl = ClassLoader.getSystemClassLoader();

	/**
	 * The system classpath.
	 */
	public static final ClassPath cp;

	/**
	 * A sorted, unmodifiable list of all class names in {@linkplain #cp the system classpath}.
	 */
	private static final List<String> systemClassNames;

	static {
		try {
			cp = ClassPath.from(scl);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}

		ArrayList<String> list = cp.getResources().stream()
				.filter(ClassPath.ClassInfo.class::isInstance)
				.map(ClassPath.ClassInfo.class::cast)
				.map(ClassPath.ClassInfo::getName)
				.map(name -> name.replace('.', '/'))
				.sorted(Comparator.naturalOrder())
				.collect(Collectors.toCollection(ArrayList::new));
		list.trimToSize();
		systemClassNames = Collections.unmodifiableList(list);
	}

	/**
	 * Returns a sorted, unmodifiable list of all class names in the system classpath.
	 */
	public static List<String> getSystemClassNames() {
		return systemClassNames;
	}

	/**
	 * Returns the class associated with the specified name, using {@linkplain #scl the system class loader}.
	 *
	 * <p> The class will not be initialized if it has not been initialized earlier.
	 * <p> This is equivalent to {@code Class.forName(className, false, ClassLoader.getSystemClassLoader())}
	 *
	 * @return class object representing the desired class
	 * @throws ClassNotFoundException if the class cannot be located by the system class loader
	 * @see Class#forName(String, boolean, ClassLoader)
	 */
	public static Class<?> getSystemClass(String className) throws ClassNotFoundException {
		return Class.forName(className, false, Classpath.scl);
	}

	/**
	 * Returns the class associated with the specified name, using {@linkplain #scl the system class loader}.
	 *
	 * <p> The class will not be initialized if it has not been initialized earlier.
	 * <p> This is equivalent to {@code Class.forName(className, false, ClassLoader.getSystemClassLoader())}
	 *
	 * @return class object representing the desired class,
	 *         or {@code null} if it cannot be located by the system class loader
	 * @see Class#forName(String, boolean, ClassLoader)
	 */
	public static Optional<Class<?>> getSystemClassIfExists(String className) {
		try {
			return Optional.of(getSystemClass(className));
		} catch (ClassNotFoundException | NullPointerException ex) {
			return Optional.empty();
		}
	}
}
