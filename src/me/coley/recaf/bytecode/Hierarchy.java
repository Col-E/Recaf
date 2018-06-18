package me.coley.recaf.bytecode;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.recaf.Input;
import me.coley.recaf.Logging;
import me.coley.recaf.config.impl.ConfASM;
import me.coley.recaf.event.ClassRenameEvent;
import me.coley.recaf.event.HierarchyMethodRenameEvent;
import me.coley.recaf.event.HierarchyMethodRenameEvent.MethodRenamed;
import me.coley.recaf.event.NewInputEvent;
import me.coley.recaf.util.Threads;

/**
 * ClassNode inheritence/MethodNode override util.
 * 
 * @author Matt
 */
// TODO: Why does this work randomly?
// TODO: Fix rename method breaking links
public class Hierarchy {
	private final static Hierarchy INSTANCE = new Hierarchy();
	private final Map<String, ClassWrapper> classMap = new ConcurrentHashMap<>();
	private final Map<String, MethodWrapper> methodMap = new ConcurrentHashMap<>();

	private Hierarchy() {
		// If the option is not enabled, this feature is disabled until a
		// restart occurs.
		if (ConfASM.instance().useLinkedMethodRenaming()) {
			Bus.subscribe(this);
		}
	}

	@Listener
	private void onNewInput(NewInputEvent input) {
		try {
			// clear old values
			classMap.clear();
			methodMap.clear();
			// generate class hierarchy
			Logging.info("Generating inheritence hierarchy");
			ExecutorService pool = Threads.pool();
			Map<String, ClassNode> classes = input.get().getClasses();
			for (String name : input.get().classes) {
				pool.execute(() -> addClass(name, classes));
			}
			Threads.waitForCompletion(pool);
			// generate method hierarchy
			pool = Threads.pool();
			Logging.info("Adding method hierarchy");
			for (ClassWrapper wrapper : classMap.values()) {
				pool.execute(() -> wrapper.linkMethods());
			}
			Threads.waitForCompletion(pool);
			Logging.info("Finished generating inheritence hierarchy");
		} catch (Exception e) {
			Logging.error(e);
		}
	}

	@Listener
	private void onClassRename(ClassRenameEvent rename) {
		String original = rename.getOriginalName();
		String replace = rename.getNewName();
		// replace
		ClassWrapper cw = classMap.remove(original);
		// get updated ClassNode instance, update wrapper's value.
		cw.setValue(Input.get().getClass(replace));
		classMap.put(replace, cw);
	}

	@Listener
	private void onMethodRenames(HierarchyMethodRenameEvent rename) {
		for (MethodRenamed mr : rename.getRenamedMethods()) {
			String owner = mr.owner.name;
			String original = mr.old;
			String replace = mr.rename;
			String keyOriginal = owner + "." + original + mr.desc;
			String keyReplace = owner + "." + replace + mr.desc;
			Logging.info("Update: " + keyOriginal + " -> " + keyReplace);
			// 1. Remove link to wrapper from map
			// 2. Update wrapper's value
			// 3. Add updated link to wrapper back to map
			MethodWrapper mw = methodMap.remove(keyOriginal);
			MethodNode mn = mr.get();
			Logging.info("Wrap: " + mw, 1);
			Logging.info("Meth: " + mn.name + mn.desc + "  - " + (mr.owner.methods.contains(mn)), 2);
			if (mw != null) {
				if (mn != null) {
					mw.setValue(mn);
				}
				methodMap.put(keyReplace, mw);
				Logging.info("put: " + keyReplace, 1);
			} else {
				Logging.error("Renamed method, failed to find get MethodWrapper instance from key");
			}
		}
	}

	/**
	 * Add class to class-map.
	 * 
	 * @param name
	 *            Class to add.
	 * @param classes
	 *            Map of ASM ClassNodes to pull values from.
	 * @return ClassWrapper associated with name.
	 */
	private ClassWrapper addClass(String name, Map<String, ClassNode> classes) {
		// skip invalid names
		if (name == null) {
			return null;
		}
		// skip nodes already added to the map
		if (classMap.containsKey(name)) {
			return classMap.get(name);
		}
		// create node and add to map
		ClassWrapper node = null;
		if (classes.containsKey(name)) {
			node = new ClassWrapper(classes.get(name));
		} else {
			node = new ReflectiveClassWrapper(name);
			// TODO: Verify that this works as intended:
			// * X implements Y, where Y is not in the loaded jar
			// * Y has mathod "action()"
			// * X overrides "action()"
			// * X's "action()" is locked because override locked Y's method
			// * Prevents renaming of core-java methods
			try {
				Class<?> cls = Class.forName(name, false, ClassLoader.getSystemClassLoader());
				for (Method method : cls.getDeclaredMethods()) {
					String desc = Type.getMethodDescriptor(method).toString();
					MethodNode dummy = new MethodNode();
					dummy.name = method.getName();
					dummy.desc = desc;
					methodMap.put(name + "." + method.getName() + desc, node.addMethod(dummy));
				}
			} catch (Exception e) {}
			node.setExternal();
		}
		classMap.put(name, node);
		// add children and parents if possible
		ClassNode cn = node.getValue();
		if (cn != null) {
			if (cn.superName != null) {
				ClassWrapper c = addClass(cn.superName, classes);
				if (c != null) {
					node.addParent(c);
				}
			}
			for (String interfac : cn.interfaces) {
				ClassWrapper i = addClass(interfac, classes);
				if (i != null) {
					node.addParent(i);
				}
			}
			// add methods
			for (MethodNode mn : cn.methods) {
				methodMap.put(name + "." + mn.name + mn.desc, node.addMethod(mn));
			}
		}
		return node;
	}

	/**
	 * @param target
	 *            MethodNode to check linkage to.
	 * @param owner
	 *            Class name.
	 * @param name
	 *            Method name.
	 * @param descriptor
	 *            Method desc.
	 * @return {@code true} if the defined method is linked to the target.
	 */
	private boolean linkedMethod(MethodNode target, String owner, String name, String descriptor) {
		String key = owner + "." + name + descriptor;
		MethodWrapper mw = methodMap.get(key);
		return mw != null && mw.isConnected(target);
	}

	/**
	 * @param target
	 *            MethodNode to check linkage to.
	 * @param owner
	 *            Class name.
	 * @param name
	 *            Method name.
	 * @param descriptor
	 *            Method desc.
	 * @return {@code true} if the defined method is linked to the target.
	 */
	public static boolean linked(MethodNode target, String owner, String name, String descriptor) {
		return INSTANCE.linkedMethod(target, owner, name, descriptor);
	}

	/**
	 * Provides access to a map of class hierarchy of ClassNodes. Keys are
	 * internal names of classes such as <i>"my/class/Name"</i>
	 * 
	 * @return Map of node wrappers for ClassNodes.
	 */
	public static Map<String, ClassWrapper> getClassMap() {
		return INSTANCE.classMap;
	}

	/**
	 * Provides access to a map of class hierarchy of ClassNodes. Keys are
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
	public static Hierarchy instance() {
		return INSTANCE;
	}

	/**
	 * Extension of Node for class-specific attributes, specifically contained
	 * methods.
	 * 
	 * @author Matt
	 */
	public static class ClassWrapper extends Node<ClassNode> {
		private final Map<String, MethodWrapper> methods = new ConcurrentHashMap<>();
		private boolean external;

		public ClassWrapper(ClassNode value) {
			super(value);
		}

		public void linkMethods() {
			for (MethodWrapper method : getMethods()) {
				method.linkMethods();
			}
		}

		public MethodWrapper addMethod(MethodNode mn) {
			MethodWrapper node = new MethodWrapper(this, mn);
			getMethodMap().put(mn.name + mn.desc, node);
			return node;
		}

		public Map<String, MethodWrapper> getMethodMap() {
			return methods;
		}

		public Collection<MethodWrapper> getMethods() {
			return getMethodMap().values();
		}

		public void setExternal() {
			external = true;
			// If the class is external, mark all methods as locked.
			// This indicates that the methods of this class and their
			// extensions should not be renamable without consequence.
			for (MethodWrapper method : getMethods()) {
				method.setLocked(true);
			}
		}

		public boolean isExternal() {
			return external;
		}
	}

	/**
	 * Extension of ClassWrapper for reflection-loaded items. This also has
	 * checks against linking all types via the common <i>"java/lang/Object"</i>
	 * parent.
	 * 
	 * @author Matt
	 */
	public static class ReflectiveClassWrapper extends ClassWrapper {
		private final String name;

		public ReflectiveClassWrapper(String name) {
			super(null);
			this.name = name;
		}

		@Override
		public boolean isConnected(ClassNode target) {
			if (name.equals("java/lang/Object")) {
				return false;
			}
			return super.isConnected(target);
		}
	}

	/**
	 * Extension of Node for method-specific functionality, specifically
	 * linking.
	 * 
	 * @author Matt
	 */
	public static class MethodWrapper extends Node<MethodNode> {
		private final ClassWrapper owner;

		public MethodWrapper(ClassWrapper owner, MethodNode value) {
			super(value);
			this.owner = owner;
		}

		public void linkMethods() {
			// If node is already linked (Such as a child or parent spawning the
			// link process) then don't do it again.
			linkMethods(getOwner(), new HashSet<ClassWrapper>());

		}

		private void linkMethods(ClassWrapper other, Set<ClassWrapper> existing) {
			// if current node searched, end
			// also, if the other wrapper is of the type 'java/lang/Object' we
			// DO NOT want to use this as a node to branch off our search from.
			// Why? Because that would bring pain and suffering.
			if (existing.contains(other) || isObjectClass(other)) {
				return;
			}
			// check match for current node
			MethodNode value = getValue();
			if (value != null) {
				MethodWrapper method = other.getMethodMap().get(value.name + value.desc);
				if (meatches(method)) {
					addChild(method);
					addParent(method);
				}
			}
			existing.add(other);
			// iterate children and parents
			for (Node<ClassNode> node : other.getChildren()) {
				linkMethods((ClassWrapper) node, existing);
			}
			for (Node<ClassNode> node : other.getParents()) {
				linkMethods((ClassWrapper) node, existing);
			}
		}

		/**
		 * @param wrap
		 * @return Check if wrapper is wrapper of <i>"java/lang/Object"</i>.
		 */
		private boolean isObjectClass(ClassWrapper wrap) {
			return wrap.getClass() == ReflectiveClassWrapper.class && ((ReflectiveClassWrapper) wrap).name.endsWith(
					"java/lang/Object");
		}

		/**
		 * @param other
		 *            Other method wrapper to check for match.
		 * @return Matching method definition <i>(name + desc)</i>
		 */
		public boolean meatches(MethodWrapper other) {
			// deny-self matching
			if (other == null) {
				return false;
			}
			MethodNode mnOther = other.getValue();
			MethodNode mnThis = this.getValue();
			return mnOther != null && mnThis != null && mnThis.name.equals(mnOther.name) && mnThis.desc.equals(mnOther.desc);
		}

		public ClassWrapper getOwner() {
			return owner;
		}
	}

	/**
	 * Map node with directional linkes <i>(Parent / Child)</i>
	 * 
	 * @author Matt
	 *
	 * @param <T>
	 *            Type of value to hold.
	 */
	public static class Node<T> {
		// using the map to derive SoncurrentHashSet usage.
		private final Map<Integer, Node<T>> parents = new ConcurrentHashMap<>();
		private final Map<Integer, Node<T>> children = new ConcurrentHashMap<>();
		private T value;
		private boolean locked;

		public Node(T value) {
			this.value = value;
		}

		/**
		 * Search for value in node map.
		 * 
		 * @param target
		 *            Value to find.
		 * @return {@code true} if value found in node map. If the value of
		 *         {@link #isLocked()} is {@code true} then this returns
		 *         {@code false}.
		 */
		public boolean isConnected(T target) {
			return !isLocked() && isConnected(target, new HashSet<T>());
		}

		/**
		 * Recursive search of all parents and children.
		 * 
		 * @param target
		 *            Value to find.
		 * @param existing
		 *            Set of already searched items.
		 * @return {@code true} if value found in node map.
		 */
		private boolean isConnected(T target, Set<T> existing) {
			// check match for current node
			if (existing.contains(target)) {
				return true;
			}
			// if current node searched, end
			if (existing.contains(getValue())) {
				return false;
			}
			existing.add(getValue());
			// scan parents and children for match
			for (Node<T> parent : getParents()) {
				if (parent.isConnected(target, existing)) {
					return true;
				}
			}
			for (Node<T> child : getChildren()) {
				if (child.isConnected(target, existing)) {
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
			if (!parents.containsValue(parent)) {
				parents.put(parent.hashCode(), parent);
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
			if (!children.containsValue(child)) {
				children.put(child.hashCode(), child);
				child.addParent(this);
			}
		}

		/**
		 * @return Parent list.
		 */
		public Collection<Node<T>> getParents() {
			return parents.values();
		}

		/**
		 * @return Child list.
		 */
		public Collection<Node<T>> getChildren() {
			return children.values();
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
			for (Node<T> parent : getParents()) {
				parent.lock(locked);
			}
			for (Node<T> child : getChildren()) {
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
