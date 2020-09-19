package me.coley.recaf.util;

import me.coley.recaf.Recaf;

import java.io.*;

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static java.lang.Class.forName;

/**
 * Classpath utility.
 *
 * @author Matt
 * @author Andy Li
 * @author xxDark
 */
public class ClasspathUtil {
	private static final String RECAF_CL = "me.coley.recaf.util.RecafClassLoader";
	/**
	 * The system classloader, provided by {@link ClassLoader#getSystemClassLoader()}.
	 */
	public static final ClassLoader scl = ClassLoader.getSystemClassLoader();

	/**
	 * A sorted, unmodifiable list of all class names
	 */
	private static final Set<String> systemClassNames;

	static {
		try {
			systemClassNames = Collections.unmodifiableSet(scanBootstrapClasses());
		} catch (Exception ex) {
			throw new ExceptionInInitializerError(ex);
		}
		if (!areBootstrapClassesFound()) {
			Log.warn("Bootstrap classes are missing!");
		}
	}

	/**
	 * Check if a resource exists in the current classpath.
	 *
	 * @param path
	 *            Path to resource.
	 * @return {@code true} if resource exists. {@code false} otherwise.
	 */
	public static boolean resourceExists(String path) {
		if (!path.startsWith("/"))
			path = "/" + path;
		return ClasspathUtil.class.getResource(path) != null;
	}

	/**
	 * Fetch a resource as a stream in the current classpath.
	 *
	 * @param path
	 * 		Path to resource.
	 *
	 * @return Stream of resource.
	 */
	public static InputStream resource(String path) {
		if (!path.startsWith("/"))
			path = "/" + path;
		return ClasspathUtil.class.getResourceAsStream(path);
	}

	/**
	 * @return A sorted, unmodifiable list of all class names in the system classpath.
	 */
	public static Set<String> getSystemClassNames() {
		return systemClassNames;
	}

	/**
	 * Checks if bootstrap classes is found in {@link #getSystemClassNames()}.
	 * @return {@code true} if they do, {@code false} if they don't
	 */
	public static boolean areBootstrapClassesFound() {
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
	 * 		The fully qualified class name.
	 *
	 * @return class object representing the desired class
	 *
	 * @throws ClassNotFoundException
	 * 		if the class cannot be located by the system class loader
	 * @see Class#forName(String, boolean, ClassLoader)
	 */
	public static Class<?> getSystemClass(String className) throws ClassNotFoundException {
		return forName(className, false, ClasspathUtil.scl);
	}

	/**
	 * Check if a class by the given name exists and is accessible by the system classloader.
	 *
	 * @param name
	 * 		The fully qualified class name.
	 *
	 * @return {@code true} if the class exists, {@code false} otherwise.
	 */
	public static boolean classExists(String name) {
		try {
			getSystemClass(name);
			return true;
		} catch(Exception ex) {
			return false;
		}
	}

	/**
	 * Returns the class associated with the specified name, using
	 * {@linkplain #scl the system class loader}.
	 * <br> The class will not be initialized if it has not been initialized earlier.
	 * <br> This is equivalent to {@code Class.forName(className, false, ClassLoader
	 * .getSystemClassLoader())}
	 *
	 * @param className
	 * 		The fully qualified class name.
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
	 * @param loader
	 * 		Loader to check.
	 *
	 * @return {@code true} if loader belongs to Recaf.
	 */
	public static boolean isRecafLoader(ClassLoader loader) {
		// Why are all good features only available in JDK9+?
		// See java.lang.ClassLoader#getName().
		if (loader == Recaf.class.getClassLoader()) {
			return true;
		}
		return loader != null && RECAF_CL.equals(loader.getClass().getName());
	}

	/**
	 * @param clazz
	 * 		Class to check.
	 *
	 * @return {@code true} if class is loaded by Recaf.
	 */
	public static boolean isRecafClass(Class<?> clazz) {
		return isRecafLoader(clazz.getClassLoader());
	}

	/**
	 * Internal utility to check if bootstrap classes exist in a list of class names.
	 */
	private static boolean checkBootstrapClassExists(Collection<String> names) {
		String name = Object.class.getName();
		return names.contains(name) || names.contains(name.replace('.', '/'));
	}

	private static Set<String> scanBootstrapClasses() throws Exception {
		int vmVersion = VMUtil.getVmVersion();
		Set<String> classes = new LinkedHashSet<>(4096, 1F);
		if (vmVersion < 9) {
			Method method = ClassLoader.class.getDeclaredMethod("getBootstrapClassPath");
			method.setAccessible(true);
			Field field = URLClassLoader.class.getDeclaredField("ucp");
			field.setAccessible(true);

			Object bootstrapClasspath = method.invoke(null);
			URLClassLoader dummyLoader = new URLClassLoader(new URL[0]);
			Field modifiers = Field.class.getDeclaredField("modifiers");
			modifiers.setAccessible(true);
			modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
			// Change the URLClassPath in the dummy loader to the bootstrap one.
			field.set(dummyLoader, bootstrapClasspath);
			URL[] urls = dummyLoader.getURLs();
			for (URL url : urls) {
				String protocol = url.getProtocol();
				JarFile jar = null;
				if ("jar".equals(protocol)) {
					jar = ((JarURLConnection)url.openConnection()).getJarFile();
				} else if ("file".equals(protocol)) {
					File file = new File(url.toURI());
					if (!file.isFile()) continue;
					jar = new JarFile(file);
				}
				if (jar == null) continue;
				try {
					Enumeration<? extends JarEntry> enumeration = jar.entries();
					while (enumeration.hasMoreElements()) {
						JarEntry entry = enumeration.nextElement();
						String name = entry.getName();
						if (name.endsWith(".class")) {
							classes.add(name.substring(0, name.length() - 6));
						}
					}
				} finally {
					jar.close();
				}
			}
			return classes;
		} else {
			Set<ModuleReference> references = ModuleFinder.ofSystem().findAll();
			for (ModuleReference ref : references) {
				try (ModuleReader mr = ref.open()) {
					mr.list().forEach(s -> {
						classes.add(s.substring(0, s.length() - 6));
					});
				}
			}
		}
		return classes;
	}
}