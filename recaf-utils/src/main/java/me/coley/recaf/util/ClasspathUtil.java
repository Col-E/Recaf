package me.coley.recaf.util;

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
	private static Tree tree;

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
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Check if a resourc exists in the current classpath.
	 *
	 * @param path
	 * 		Path to resource.
	 *
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
	 * @return List of package names belonging to the core JDK.
	 */
	public static List<String> getSystemPackages() {
		return ModuleFinder.ofSystem().findAll().stream()
				.flatMap(moduleReference -> moduleReference.descriptor().exports().stream())
				.map(ModuleDescriptor.Exports::source)
				.distinct()
				.sorted()
				.collect(Collectors.toList());
	}

	/**
	 * @return Tree representation of all system classes.
	 */
	public static Tree getSystemClasses() {
		if (tree == null) {
			tree = new Tree(null, "");
			List<String> classes = ModuleFinder.ofSystem().findAll().stream()
					.map(Unchecked.function(ModuleReference::open))
					.flatMap(Unchecked.function(ModuleReader::list))
					.filter(s -> s.endsWith(".class") && s.indexOf('-') == -1)
					.map(s -> s.substring(0, s.length() - 6))
					.collect(Collectors.toList());
			for (String className : classes)
				tree.visitPath(className);
			tree.freeze();
		}
		return tree;
	}

	/**
	 * Tree node.
	 *
	 * @author Matt Coley
	 */
	public static class Tree implements Comparable<Tree> {
		private final Tree parent;
		private final String value;
		private Map<String, Tree> children;
		private boolean frozen;

		/**
		 * @param parent
		 * 		Parent tree node.
		 * @param value
		 * 		Local path item.
		 */
		public Tree(Tree parent, String value) {
			this.parent = parent;
			this.value = value;
		}

		/**
		 * Freeze tree from changes.
		 */
		private void freeze() {
			frozen = true;
			if (children != null)
				children.values().forEach(Tree::freeze);
		}

		/**
		 * @param child
		 * 		Child path item.
		 *
		 * @return Child tree.
		 */
		public Tree visit(String child) {
			if (children == null)
				children = new HashMap<>();
			if (frozen)
				return children.get(child);
			return children.computeIfAbsent(child, c -> new Tree(this, c));
		}

		/**
		 * @param path
		 * 		Child path. Multiple path items are separated by the {@code /} character.
		 *
		 * @return Child tree.
		 */
		public Tree visitPath(String path) {
			String[] parts = path.split("/");
			return visitPath(parts);
		}

		/**
		 * @param path
		 * 		Child path items.
		 *
		 * @return Child tree, or {@code null} if no such sub-path exists.
		 */
		public Tree visitPath(String... path) {
			Tree node = this;
			for (String part : path) {
				// If this is an unknown path (not belonging to children map) and the tree is frozen
				// then 'subtree' will be null. Frozen trees are now allowed to create new nodes.
				Tree subtree = node.visit(part);
				if (subtree == null)
					return null;
				node = subtree;
			}
			return node;
		}

		/**
		 * @return Direct leaves of this node.
		 */
		public List<Tree> getBranches() {
			if (children == null)
				return Collections.singletonList(this);
			List<Tree> values = new ArrayList<>();
			children.values().stream()
					.filter(Tree::isBranch)
					.forEach(values::add);
			return values;
		}

		/**
		 * @return Direct leaves of this node.
		 */
		public List<Tree> getLeaves() {
			if (children == null)
				return Collections.singletonList(this);
			List<Tree> values = new ArrayList<>();
			children.values().stream()
					.filter(Tree::isLeaf)
					.forEach(values::add);
			return values;
		}

		/**
		 * @return All leaves accessible from this node.
		 */
		public List<Tree> getAllLeaves() {
			if (children == null)
				return Collections.singletonList(this);
			List<Tree> values = new ArrayList<>();
			children.values().forEach(t -> values.addAll(t.getAllLeaves()));
			return values;
		}

		/**
		 * @return Direct tree nodes accessible from this node.
		 */
		public Map<String, Tree> getChildren() {
			if (children == null)
				return Collections.emptyMap();
			return children;
		}

		/**
		 * @return {@code true} for the root node.
		 */
		public boolean isRoot() {
			return parent == null;
		}

		/**
		 * @return {@code true} when there are no children in this node.
		 */
		public boolean isLeaf() {
			return children == null;
		}

		/**
		 * @return {@code true} when there are children in this node.
		 */
		public boolean isBranch() {
			return !isLeaf();
		}

		/**
		 * @return Parent tree node.
		 */
		public Tree getParent() {
			return parent;
		}

		/**
		 * @return Local path item.
		 */
		public String getValue() {
			return value;
		}

		/**
		 * @return Full path.
		 */
		public String getFullValue() {
			if (parent == null)
				return getValue();
			else if (parent.isRoot())
				return getValue();
			else
				return parent.getFullValue() + "/" + getValue();
		}

		@Override
		public int compareTo(Tree o) {
			return getFullValue().compareTo(o.getFullValue());
		}

		@Override
		public String toString() {
			return getFullValue();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Tree tree = (Tree) o;
			return Objects.equals(parent, tree.parent) && value.equals(tree.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(parent, value);
		}
	}
}
