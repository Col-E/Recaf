package me.coley.recaf.util;

import com.google.common.base.Strings;
import com.google.common.reflect.ClassPath;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Classpath utility.
 *
 * @author Matt
 * @author Andy Li
 */
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
		ClassPathScanner scanner = new ClassPathScanner();
		scanner.scan(scl);
		cp = scanner.classPath;
		systemClassNames = Collections.unmodifiableList(scanner.internalNames);
	}

	/**
	 * @return A sorted, unmodifiable list of all class names in the system classpath.
	 */
	public static List<String> getSystemClassNames() {
		return systemClassNames;
	}

	/**
	 * Checks if bootstrap classes is found in {@link #getSystemClassNames()}.
	 * @return {@code true} if they do, {@code false} if they don't
	 */
	public static boolean isBootstrapClassesFound() {
		return checkBootstrapClassExists(getSystemClassNames());
	}

	/**
	 * Returns the class associated with the specified name, using
	 * {@linkplain #scl the system class loader}.
	 * <br> The class will not be initialized if it has not been initialized earlier.
	 * <br> This is equivalent to {@code Class.forName(className, false, ClassLoader
	 * .getSystemClassLoader())}
	 *
	 * @param className
	 * 		The fully quantified class name.
	 *
	 * @return class object representing the desired class
	 *
	 * @throws ClassNotFoundException
	 * 		if the class cannot be located by the system class loader
	 * @see Class#forName(String, boolean, ClassLoader)
	 */
	public static Class<?> getSystemClass(String className) throws ClassNotFoundException {
		return Class.forName(className, false, Classpath.scl);
	}

	/**
	 * Returns the class associated with the specified name, using
	 * {@linkplain #scl the system class loader}.
	 * <br> The class will not be initialized if it has not been initialized earlier.
	 * <br> This is equivalent to {@code Class.forName(className, false, ClassLoader
	 * .getSystemClassLoader())}
	 *
	 * @param className
	 * 		The fully quantified class name.
	 *
	 * @return class object representing the desired class,
	 * or {@code null} if it cannot be located by the system class loader
	 *
	 * @see Class#forName(String, boolean, ClassLoader)
	 */
	public static Optional<Class<?>> getSystemClassIfExists(String className) {
		try {
			return Optional.of(getSystemClass(className));
		} catch (ClassNotFoundException | NullPointerException ex) {
			return Optional.empty();
		}
	}

	/**
	 * Internal utility to check if bootstrap classes exist in a list of class names.
	 */
	private static boolean checkBootstrapClassExists(Collection<String> names) {
		String name = Object.class.getName();
		return names.contains(name) || names.contains(name.replace('.', '/'));
	}

	/**
	 * Utility class for easy state management.
	 */
	private static class ClassPathScanner {
		public ClassPath classPath;
		public List<String> names;
		public List<String> internalNames;

		private void updateClassPath(ClassLoader loader) {
			try {
				classPath = ClassPath.from(loader);
				names = classPath.getResources().stream()
						.filter(ClassPath.ClassInfo.class::isInstance)
						.map(ClassPath.ClassInfo.class::cast)
						.map(ClassPath.ClassInfo::getName)
						.collect(Collectors.toCollection(ArrayList::new));
			} catch (IOException e) {
				throw new UncheckedIOException("Unable to scan classpath entries: " +
						loader.getClass().getName(), e);
			}
		}

		private boolean checkBootstrapClass() {
			return checkBootstrapClassExists(names);
		}

		public void scan(ClassLoader classLoader) {
			updateClassPath(classLoader);

			// In some JVM implementation, the bootstrap class loader is implemented directly in native code
			// and does not exist as a ClassLoader instance. Unfortunately, Oracle JVM is one of them.
			//
			// Considering Guava's ClassPath util works (and can only work) by scanning urls from an URLClassLoader,
			// this means it cannot find any of the standard API like java.lang.Object
			// without explicitly specifying `-classpath=rt.jar` etc. in the launch arguments
			// (only IDEs' Run/Debug seems to do that automatically)
			//
			// It further means that in most of the circumstances (including `java -jar recaf.jar`)
			// auto-completion will not able to suggest internal names of any of the classes under java.*,
			// which will largely reduce the effectiveness of the feature.
			if (!checkBootstrapClass()) {

				// The classpath for bootstrap classes can be found
				// in the "sun.boot.class.path" property (assuming it's Oracle JVM).
				// This was removed in Oracle JVM 9, and all related code was refactored.
				// I use it to indicate whether the following method is supported or not.
				if (!Strings.isNullOrEmpty(System.getProperty("sun.boot.class.path"))) {
					try {
						Method method = ClassLoader.class.getDeclaredMethod("getBootstrapClassPath");
						method.setAccessible(true);
						Field field = URLClassLoader.class.getDeclaredField("ucp");
						field.setAccessible(true);

						Object bootstrapClasspath = method.invoke(null);
						URLClassLoader dummyLoader = new URLClassLoader(new URL[0], classLoader);
						// Change the URLClassPath in the dummy loader to the bootstrap one.
						field.set(dummyLoader, bootstrapClasspath);
						// And then feed it into Guava's ClassPath scanner.
						updateClassPath(dummyLoader);

						if (!checkBootstrapClass()) {
							Logger.warn("Bootstrap classes are (still) missing from the classpath scan!");
						}
					} catch (ReflectiveOperationException | SecurityException e) {
						throw new ExceptionInInitializerError(e);
					}
				} else {
					// The internal implementation, including the class loading mechanism,
					// was completely redesigned in Java 9. And after some hours of research,
					// I believe that it was theoretically impossible to acquire the bootstrap classpath anymore -
					// it seems to be completely native, and doesn't show up anywhere in the code.
					Logger.warn("Recaf cannot acquire the classpath for bootstrap classes when running in Java 9 or above. " +
							"This will affect auto-complete suggestions in Assembler!");
				}
			}

			// Map to internal names
			internalNames = names.stream()
					.map(name -> name.replace('.', '/'))
					.sorted(Comparator.naturalOrder())
					.collect(Collectors.toCollection(ArrayList::new));
			((ArrayList<String>) internalNames).trimToSize();
		}
	}
}