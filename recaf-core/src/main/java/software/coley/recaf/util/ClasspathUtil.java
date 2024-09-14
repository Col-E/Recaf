package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.collections.Unchecked;
import software.coley.collections.tree.SortedTree;
import software.coley.collections.tree.SortedTreeImpl;
import software.coley.collections.tree.Tree;

import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Class.forName;

/**
 * Classpath utility.
 *
 * @author Matt Coley
 * @author Andy Li
 * @author xDark
 */
public class ClasspathUtil {
	/**
	 * The system classloader, provided by {@link ClassLoader#getSystemClassLoader()}.
	 */
	public static final ClassLoader scl = ClassLoader.getSystemClassLoader();
	/**
	 * Cache of all system classes represented as a tree.
	 */
	private static volatile Tree<String, String> classTree;
	/**
	 * Cache of all system classes represented as a set.
	 */
	private static volatile NavigableSet<String> classSet;
	/**
	 * Cached list of available system packages.
	 */
	private static volatile List<String> systemPackages;

	/**
	 * Returns the class associated with the specified name, using
	 * {@link #scl the system class loader}.
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
	@Nonnull
	public static Class<?> getSystemClass(@Nonnull String className) throws ClassNotFoundException {
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
	public static boolean classExists(@Nonnull String name) {
		try {
			getSystemClass(name);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Check if a resource exists in the current classpath.
	 *
	 * @param path
	 * 		Path to resource.
	 *
	 * @return {@code true} if resource exists. {@code false} otherwise.
	 */
	public static boolean resourceExists(@Nonnull String path) {
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
	@Nullable
	public static InputStream resource(String path) {
		if (!path.startsWith("/"))
			path = "/" + path;
		return ClasspathUtil.class.getResourceAsStream(path);
	}

	/**
	 * @return List of package names belonging to the core JDK.
	 */
	@Nonnull
	public static List<String> getSystemPackages() {
		if (systemPackages == null) {
			synchronized (ClasspathUtil.class) {
				if (systemPackages == null) {
					systemPackages = ModuleFinder.ofSystem().findAll().stream()
							.flatMap(moduleReference -> moduleReference.descriptor().exports().stream())
							.map(ModuleDescriptor.Exports::source)
							.distinct()
							.collect(Collectors.toList());
				}
			}
		}
		return systemPackages;
	}

	/**
	 * @return Set of all system classes.
	 */
	@Nonnull
	public static NavigableSet<String> getSystemClassSet() {
		if (classSet == null) {
			synchronized (ClasspathUtil.class) {
				if (classSet == null) {
					classSet = new TreeSet<>();
					ModuleFinder.ofSystem().findAll().stream()
							.map(Unchecked.function(ModuleReference::open))
							.flatMap(Unchecked.function(ModuleReader::list))
							.filter(s -> s.endsWith(".class") && s.indexOf('-') == -1)
							.map(s -> s.substring(0, s.length() - 6))
							.forEach(s -> classSet.add(s));
				}
			}
		}
		return classSet;
	}

	/**
	 * @return Tree representation of all system classes.
	 */
	@Nonnull
	public static Tree<String, String> getSystemClassTree() {
		if (classTree == null) {
			synchronized (ClasspathUtil.class) {
				if (classTree == null) {
					classTree = new SortedTreeImpl<>();
					ModuleFinder.ofSystem().findAll().stream()
							.map(Unchecked.function(ModuleReference::open))
							.flatMap(Unchecked.function(ModuleReader::list))
							.filter(s -> s.endsWith(".class") && s.indexOf('-') == -1)
							.map(s -> s.substring(0, s.length() - 6))
							.forEach(s -> {
								String[] sections = s.split("/");
								Tree<String, String> path = classTree;
								for (String section : sections) {
									SortedTree<String, String> copy = (SortedTree<String, String>) path;
									path = path.computeIfAbsent(section, x -> new SortedTreeImpl<>(copy, x));
								}
							});
				}
			}
		}
		return classTree;
	}
}
