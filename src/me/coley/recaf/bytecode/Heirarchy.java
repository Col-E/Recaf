package me.coley.recaf.bytecode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.recaf.Logging;
import me.coley.recaf.event.NewInputEvent;

/**
 * ClassNode inheritence/MethodNode override util.
 * 
 * @author Matt
 */
public class Heirarchy {
	private final static Heirarchy INSTANCE = new Heirarchy();
	private final Map<String, ClassWrapper> classMap = new HashMap<>();
	private final Map<String, MethodWrapper> methodMap = new HashMap<>();

	private Heirarchy() {
		Bus.subscribe(this);
	}

	// TODO: Have hooks for class / method renames
	// recalculate parts of the maps only as needed.
	//
	// * Add new names to keyset
	// * Replace value in node
	// * Check if any parents/children were removed.

	@Listener
	private void onNewInput(NewInputEvent input) {
		try {
			Logging.info("Generating inheritence heirarchy");
			Map<String, ClassNode> classes = input.get().getClasses();
			for (String name : input.get().classes) {
				addClass(name, classes);
			}
			Logging.info("Adding method heirarchy");

			Logging.info("Finished generating inheritence heirarchy");
		} catch (Exception e) {
			Logging.error(e);
		}
	}
	
	// TODO: Add MethodWrapper items
	// * could just finish ClassNode map, then do spanning searches for every possible method
	//   and then link the results.
	// * could also add method sub-nodes to the class-node subs. Restructure so node types are
	//   explicit 
	//      * ClassWrapper extends ClassWrapper
	//      * MethodWrapper extends MethodWrapper 
	//      * ClassWrapper.methods of type ^

	private ClassWrapper addClass(String name, Map<String, ClassNode> classes) {
		// skip nodes already added to the map
		if (classMap.containsKey(name)) {
			return classMap.get(name);
		}
		// create node and add to map
		ClassWrapper node = null;
		if (classes.containsKey(name)) {
			node = new ClassWrapper(classes.get(name));
		} else {
			node = new ClassWrapper(null);
		}
		classMap.put(name, node);
		// add children and parents if possible
		ClassNode cn = node.getValue();
		if (cn != null) {
			node.addParent(addClass(cn.superName, classes));
			for (String interfac : cn.interfaces) {
				node.addParent(addClass(interfac, classes));
			}
			// add methods
			for (MethodNode mn : cn.methods) {
				node.getMethods().add(new MethodWrapper(mn));
			}
		}
		return node;
	}

	/**
	 * Provides access to a map of class heirarchy of ClassNodes. Keys are
	 * internal names of classes such as <i>"my/class/Name"</i>
	 * 
	 * @return Map of node wrappers for ClassNodes.
	 */
	public static Map<String, ClassWrapper> getClassMap() {
		return INSTANCE.classMap;
	}

	/**
	 * Provides access to a map of class heirarchy of ClassNodes. Keys are
	 * internal names of classes such as <i>"my/class/Name"</i>
	 * 
	 * @return Map of node wrappers for ClassNodes.
	 */
	public static Map<String, MethodWrapper> getMethodMap() {
		return INSTANCE.methodMap;
	}
	
	/**
	 * @return static instance.
	 */
	public static Heirarchy instance() {
		return INSTANCE;
	}
	
	public static class ClassWrapper extends Node<ClassNode> {
		private final List<MethodWrapper> methods = new ArrayList<>();
		
		public ClassWrapper(ClassNode value) {
			super(value);
		}
		
		public List<MethodWrapper> getMethods()  {
			return methods;
		}
	}
	
	public static class MethodWrapper extends Node<MethodNode> {
		public MethodWrapper(MethodNode value) {
			super(value);
		}
	}

	public static class Node<T> {
		private final List<Node<T>> parents = new ArrayList<>();
		private final List<Node<T>> children = new ArrayList<>();
		private T value;
		private boolean locked;

		public Node(T value) {
			this.value = value;
		}

		/**
		 * Search for value in node map.
		 * 
		 * @param other
		 *            Value to find.
		 * @return {@code true} if value found in node map.
		 */
		public boolean isConnected(T other) {
			return isConnected(other, new HashSet<T>(Collections.singleton(getValue())));
		}

		/**
		 * Recursive search of all parents and children.
		 * 
		 * @param other
		 *            Value to find.
		 * @param existing
		 *            Set of already searched items.
		 * @return {@code true} if value found in node map.
		 */
		private boolean isConnected(T other, Set<T> existing) {
			// check match for current node
			if (existing.contains(other)) {
				return true;
			}
			// if current node searched, end
			if (existing.contains(getValue())) {
				return false;
			}
			existing.add(getValue());
			// scan parents and children for match
			for (Node<T> parent : parents) {
				if (parent.isConnected(other, existing)) {
					return true;
				}
			}
			for (Node<T> child : children) {
				if (child.isConnected(other, existing)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * @return Value associated with node.
		 */
		public T getValue() {
			return value;
		}

		/**
		 * @param value
		 *            Value to set.
		 */
		public void setValue(T value) {
			this.value = value;
		}

		/**
		 * Add node to map.
		 * 
		 * @param parent
		 *            Node representing a parent of this node.
		 */
		public void addParent(Node<T> parent) {
			if (!parents.contains(parent)) {
				parents.add(parent);
				parent.addChild(this);
			}
		}

		/**
		 * Add node to map.
		 * 
		 * @param child
		 *            Node representing a child of this node.
		 */
		public void addChild(Node<T> child) {
			if (!children.contains(child)) {
				children.add(child);
				child.addParent(this);
			}
		}

		/**
		 * @return Parent list.
		 */
		public List<Node<T>> getParents() {
			return parents;
		}

		/**
		 * @return Child list.
		 */
		public List<Node<T>> getChildren() {
			return children;
		}

		/**
		 * Set lock value for node map.
		 * 
		 * @param locked
		 *            Value to set.
		 */
		public void setLocked(boolean locked) {
			lock(locked);
		}

		/**
		 * Recursive lock of all children and parents.
		 * 
		 * @param locked
		 *            Value to set.
		 */
		private void lock(boolean locked) {
			// check already locked
			if (this.isLocked() == locked) {
				return;
			}
			// update lock
			this.locked = locked;
			// lock parents and children
			for (Node<T> parent : parents) {
				parent.lock(locked);
			}
			for (Node<T> child : children) {
				child.lock(locked);
			}
		}

		/**
		 * @return Node is locked, implying changes to the value should be
		 *         limited.
		 */
		public boolean isLocked() {
			return locked;
		}
	}
}
