package me.coley.recaf.bytecode.analysis;

import java.lang.reflect.Method;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.Objects;
import java.util.Set;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import com.google.common.collect.Iterables;

import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.recaf.Input;
import me.coley.recaf.Logging;
import me.coley.recaf.config.impl.ConfASM;
import me.coley.recaf.event.ClassRenameEvent;
import me.coley.recaf.event.MethodRenameEvent;
import me.coley.recaf.event.NewInputEvent;
import me.coley.recaf.util.Threads;

/**
 * ClassNode inheritance/MethodNode override utility.
 * 
 * @author Matt
 */
// TODO: Update when user does the following:
// - Update interface list
// - Update superName
// - Insert method / change method name to match one in the tree
public enum Hierarchy {
	INSTANCE;
	/**
	 * Key: Class name.<br>
	 * Value: Class wrapper as graph vertex.
	 */
	private final Map<String, CVert> classes = new HashMap<>();
	/**
	 * Key: String representation of NameType.<br>
	 * Value: NameType instance.
	 */
	private final Map<String, NameType> types = new HashMap<>();
	/**
	 * Key: String representation of NameType.<br>
	 * Value: Set of groups of the NameType. Each group belongs to a different
	 * collection of classes.
	 */
	private final Map<String, Set<MGroup>> groupMap = new HashMap<>();
	/**
	 * Set of classes already visited during hierarchy generation.
	 */
	private final Set<CVert> visitedGroupHosts = new HashSet<>();
	/**
	 * Status of what has been loaded.
	 */
	private LoadStatus status = LoadStatus.NONE;

	private Hierarchy() {
		// If the option is not enabled, this feature is disabled until a
		// restart occurs.
		if (ConfASM.instance().useLinkedMethodRenaming()) {
			Bus.subscribe(this);
		}
	}

	/**
	 * Check if two methods are linked.
	 * 
	 * @param mOwner
	 *            Renamed method's owner.
	 * @param mName
	 *            Renamed method's current name <i>(Not the new name)</i>
	 * @param mDesc
	 *            Renamed method's current descriptor.
	 * @param owner
	 *            Some declared method's owner.
	 * @param name
	 *            Some declared method's name.
	 * @param desc
	 *            Some declared method's descriptor.
	 * @return {@code true} if the two methods belong to the same hierarchy.
	 */
	public boolean linked(String mOwner, String mName, String mDesc, String owner, String name, String desc) {
		// Get types, check for equality
		NameType mType = type(mName, mDesc);
		NameType type = type(name, desc);
		if (mType.equals(type)) {
			// Get groups, check for same-reference.
			MGroup mGroup = getGroup(mType, mOwner);
			MGroup group = getGroup(type, owner);
			if (group == mGroup) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if a method belongs to a method-group that has been marked as
	 * locked. This occurs in cases where the method has been detected to extend
	 * a method that is outside of the loaded input <i>(The core classes)</i>.
	 * Essentially this will prevent renaming of things like:
	 * <ul>
	 * <li>toString()</li>
	 * <li>hashCode()</li>
	 * <li>etc.</li>
	 * </ul>
	 * 
	 * @param owner
	 *            Method owner.
	 * @param name
	 *            Method name.
	 * @param desc
	 *            Method descriptor.
	 * @return {@code true} if the method belongs to a locked group.
	 */
	public boolean isLocked(String owner, String name, String desc) {
		// Check get group by type/owner
		NameType type = type(name, desc);
		MGroup group = getGroup(type, owner);
		if (group != null) {
			return group.locked;
		}
		return false;
	}

	@Listener
	private void onClassRename(ClassRenameEvent rename) {
		String original = rename.getOriginalName();
		String replace = rename.getNewName();
		// If a class is renamed, remove it from the lookup and add it back
		// under the new name.
		CVert vert = classes.get(original);
		if (vert != null) {
			classes.remove(original);
			classes.put(replace, vert);
			// As long as the event priority causes this event to be handled by
			// Input first, this will work.
			vert.data = Input.get().getClass(replace);
		}
	}

	@Listener
	private void onMethodRenamed(MethodRenameEvent rename) {
		// Get group from type
		NameType type = type(rename.getOriginalName(), rename.getMethod().desc);
		MGroup group = getGroup(type, rename.getOwner().name);
		if (group == null) {
			throw new RuntimeException("Failed to update method-hierarchy: Failed to get method-group");
		}
		// Rename group
		group.setName(rename.getNewName());
	}

	@Listener
	private void onNewInput(NewInputEvent input) {
		// Reset values
		classes.clear();
		groupMap.clear();
		types.clear();
		visitedGroupHosts.clear();
		Threads.run(() -> {
			try {
				long start = System.currentTimeMillis();
				status = LoadStatus.NONE;
				Logging.info("Generating inheritence hierarchy");
				setupVertices(input.get().getClasses());
				setupEdges();
				status = LoadStatus.CLASSES;
				setupNameType();
				setupMethodGroups();
				setupMethodLocks();
				status = LoadStatus.METHODS;
				long now = System.currentTimeMillis();
				Logging.info("Finished generating inheritence hierarchy: took " + (now - start) + "ms");
			} catch (Exception e) {
				Logging.error(e);
			}
		});
	}

	/**
	 * Setup CVert lookup for all ClassNodes.
	 * 
	 * @param nodes
	 *            Map of ClassNodes.
	 */
	private void setupVertices(Map<String, ClassNode> nodes) {
		ExecutorService pool = Threads.pool(Threads.PoolKind.IO);
		// Typically iterating the keyset and fetching the value is a bad idea.
		// But due to the nature of lazy-loading and the ability to multithread
		// the loads, this should be faster.
		for (String name : nodes.keySet()) {
			pool.execute(() -> {
				ClassNode cn = nodes.get(name);
				classes.put(cn.name, new CVert(cn));
			});
		}
		Threads.waitForCompletion(pool);
	}

	/**
	 * Setup parent-child relations in the {@link #classes CVert map} based on
	 * superclass/interface relations of the wrapped ClassNode value.
	 */
	private void setupEdges() {
		for (CVert vert : classes.values()) {
			// Get superclass vertex
			CVert other = classes.get(vert.data.superName);
			if (other != null) {
				vert.parents.add(other);
				other.children.add(vert);
			} else {
				// Could not find superclass, note that for later.
				vert.externalParents.add(vert.data.superName);
			}
			// Iterate interfaces, get interface vertex and do the same
			for (String inter : vert.data.interfaces) {
				other = classes.get(inter);
				if (other != null) {
					vert.parents.add(other);
					other.children.add(vert);
				} else {
					// Could not find superclass, note that for later.
					vert.externalParents.add(inter);
				}
			}
		}
	}

	/**
	 * Setup NameType lookup for all methods.
	 */
	private void setupNameType() {
		for (CVert vert : classes.values()) {
			for (MethodNode mn : vert.data.methods) {
				type(mn);
			}
		}
	}

	/**
	 * Setup method-groups. Requires all types and edges to be generated first.
	 */
	private void setupMethodGroups() {
		for (CVert cv : classes.values()) {
			createGroups(cv);
		}
	}

	private void createGroups(CVert root) {
		// Check if the vertex has been visited already.
		if (visitedGroupHosts.contains(root)) {
			return;
		}
		// Track visited vertices / queue vertices to visit later.
		Deque<CVert> queue = new LinkedList<>();
		Set<CVert> visited = new HashSet<>();
		// Since this will visit all classes in the to-be generated groups we
		// can make a map that will contain the NameTypes of these groups and
		// link them with the groups.
		Map<NameType, MGroup> typeGroups = new HashMap<>();
		// Initial vertex
		queue.add(root);
		visited.add(root);
		while (!queue.isEmpty()) {
			CVert node = queue.poll();
			// Mark so that future calls to createGroups can be skipped.
			visitedGroupHosts.add(node);
			// Iterate over methods, fetch the group by the NameType and then
			// add the current vertext to the group.
			for (MethodNode mn : node.data.methods) {
				NameType type = type(mn);
				MGroup group = typeGroups.get(type);
				if (group == null) {
					group = new MGroup(type);
					typeGroups.put(type, group);
				}
				group.definers.add(node);
			}
			// Queue up the unvisited parent and child nodes.
			for (CVert other : Iterables.concat(node.parents, node.children)) {
				if (other != null && !visited.contains(other)) {
					queue.add(other);
					visited.add(other);
				}
			}
		}
		// For every NameType/Group add to the main group-map.
		for (Entry<NameType, MGroup> e : typeGroups.entrySet()) {
			Set<MGroup> groups = groupMap.get(e.getKey().toString());
			if (groups == null) {
				groups = new HashSet<>();
				groupMap.put(e.getKey().toString(), groups);
			}
			groups.add(e.getValue());
		}
	}

	/**
	 * @param type
	 *            NameType of a method.
	 * @param owner
	 *            Class that has declared the method.
	 * @return Group representing method by the given NameType, group also
	 *         contains the given owner as a defining class of the method. May
	 *         be {@code null} if the owner does not declare a method by the
	 *         given NameType.
	 */
	private MGroup getGroup(NameType type, String owner) {
		CVert vert = classes.get(owner);
		if (vert == null) {
			throw new RuntimeException("Cannot fetch-group with passed owner: null");
		}
		Set<MGroup> groupSet = groupMap.get(type.toString());
		if (groupSet != null) {
			// Check for owner match.
			for (MGroup group : groupSet) {
				if (group.definers.contains(vert)) {
					return group;
				}
			}
		}
		return null;
	}

	/**
	 * Recall that in {@link #setupEdges()} that edges to external classes were
	 * logged. Now we will iterate over those external names and check if they
	 * can be loaded via Reflection. If so we will check the methods in these
	 * classes and lock all groups with matching NameTypes to prevent renaming
	 * of core-methods.
	 */
	private void setupMethodLocks() {
		// config says not to do this.
		if (!ConfASM.instance().doLockLibraryMethod()) {
			return;
		}
		Set<String> externalRefs = new HashSet<>();
		for (CVert cv : classes.values()) {
			for (String external : cv.externalParents) {
				if (external != null) {
					externalRefs.add(external);
				}
			}
		}
		ExecutorService pool = Threads.pool(Threads.PoolKind.LOGIC);
		for (String external : externalRefs) {
			pool.execute(() -> {
				String className = external.replace("/", ".");
				try {
					// Load class without initialization
					Class<?> cls = Class.forName(className, false, ClassLoader.getSystemClassLoader());
					for (Method method : cls.getDeclaredMethods()) {
						// Get NameType from method
						String name = method.getName();
						String desc = Type.getMethodDescriptor(method).toString();
						NameType type = type(name, desc);
						// Lock groups
						Set<MGroup> groups = groupMap.get(type.toString());
						if (groups != null) {
							for (MGroup group : groups) {
								group.locked = true;
							}
						}
					}
				} catch (Exception e) {}
			});
		}
		Threads.waitForCompletion(pool);
	}

	/**
	 * @param mn
	 *            MethodNode.
	 * @return NameType of MethodNode.
	 */
	private NameType type(MethodNode mn) {
		return type(mn.name + mn.desc, mn.name, mn.desc);
	}

	/**
	 * @param name
	 *            Method name.
	 * @param desc
	 *            Method descriptor.
	 * @return NameType of declared method.
	 */
	private NameType type(String name, String desc) {
		return type(name + desc, name, desc);
	}

	/**
	 * 
	 * @param def
	 *            Key for NameType lookup.
	 * @param name
	 *            Method name.
	 * @param desc
	 *            Method descriptor.
	 * @return NameType of declared method.
	 */
	private NameType type(String def, String name, String desc) {
		NameType type = types.get(def);
		if (type == null) {
			type = new NameType(name, desc);
			types.put(def, type);
		}
		return type;
	}

	/**
	 * @return Content of hierarchy loaded.
	 */
	public static LoadStatus getStatus() {
		return INSTANCE.status;
	}

	/**
	 * Class Vertex. Edges denote parent/child relations.
	 * 
	 * @author Matt
	 */
	class CVert {
		final Set<String> externalParents = new HashSet<>();
		final Set<CVert> parents = new HashSet<>();
		final Set<CVert> children = new HashSet<>();
		ClassNode data;

		CVert(ClassNode data) {
			this.data = data;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(data.name);
		}

		@Override
		public String toString() {
			return data.name;
		}
	}

	/**
	 * Method-Group. For some method defined by a NameType, hold the set of
	 * classes that define that method.
	 * 
	 * @author Matt
	 */
	class MGroup {
		final NameType type;
		final Set<CVert> definers = new HashSet<>();
		boolean locked;

		MGroup(NameType type) {
			this.type = type.copy();
		}

		public void setName(String newName) {
			if (locked) {
				throw new RuntimeException("Cannot rename a locked method-group!");
			}
			if (!groupMap.get(type.toString()).remove(this)) {
				throw new RuntimeException("Failed to remove outdated method-group from map!");
			}
			type.setName(newName);
			Set<MGroup> groups = groupMap.get(type.toString());
			if (groups == null) {
				groups = new HashSet<>();
				groupMap.put(type.toString(), groups);
			}
			groups.add(this);
		}
	}

	/**
	 * Name + Descriptor wrapper.
	 * 
	 * @author Matt
	 */
	class NameType {
		private final String desc;
		private String name;
		private String def;

		NameType(String name, String desc) {
			this.name = name;
			this.desc = desc;
			this.def = name + desc;
		}

		public NameType copy() {
			return new NameType(name, desc);
		}

		public void setName(String newName) {
			this.name = newName;
			this.def = name + desc;
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof NameType && other.toString().equals(toString())) {
				return true;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(def);
		}

		@Override
		public String toString() {
			return def;
		}
	}

	/**
	 * Status to define what has been loaded.
	 * 
	 * @author Matt
	 */
	public enum LoadStatus {
		/**
		 * Nothing has been loaded.
		 */
		NONE,
		/**
		 * Class hierarchy has been loaded.
		 */
		CLASSES,
		/**
		 * Class + method hierarchies have been loaded
		 */
		METHODS;
	}
}
