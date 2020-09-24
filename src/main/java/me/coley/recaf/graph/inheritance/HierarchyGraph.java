package me.coley.recaf.graph.inheritance;

import me.coley.recaf.graph.*;
import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.ClassReader;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Stream.*;

/**
 * Graph model to represent the class inheritance of a loaded input. <br>
 * The graph is generative, meaning the graph's vertices and edges are dynamically generated when
 * requested. The relations are not stored due to their modifiable nature.
 *
 * @author Matt
 */
public class HierarchyGraph extends WorkspaceGraph<HierarchyVertex> {
	/**
	 * Map of parent to children names.
	 */
	private final Map<String, Set<String>> descendents = new HashMap<>();

	/**
	 * Constructs a hierarchy graph from the given workspace.
	 *
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	public HierarchyGraph(Workspace workspace) {
		super(workspace);
		refresh();
	}

	@Override
	public HierarchyVertex getVertex(ClassReader key) {
		return getVertexFast(key);
	}

	@Override
	public HierarchyVertex getVertexFast(ClassReader key) {
		return new HierarchyVertex(this, key);
	}

	/**
	 * @param name
	 * 		Class name of a class belonging to some inheritance hierarchy.
	 *
	 * @return Inheritance hierarchy containing the given class.
	 */
	public Set<HierarchyVertex> getHierarchy(String name) {
		return getHierarchy(getVertex(name));
	}

	/**
	 * @param vertex
	 * 		Class vertex that belongs to some inheritance hierarchy.
	 *
	 * @return Inheritance hierarchy containing the given class.
	 */
	public Set<HierarchyVertex> getHierarchy(HierarchyVertex vertex) {
		if(vertex == null)
			return Collections.emptySet();
		ClassHierarchyBuilder builder = new ClassHierarchyBuilder();
		return builder.build(vertex);
	}

	/**
	 * @param name
	 * 		Class name of a class belonging to some inheritance hierarchy.
	 *
	 * @return Inheritance hierarchy containing the given class.
	 */
	public Set<String> getHierarchyNames(String name) {
		return getHierarchyNames(getVertex(name));
	}

	/**
	 * @param vertex
	 * 		Class vertex that belongs to some inheritance hierarchy.
	 *
	 * @return Inheritance hierarchy containing the given class.
	 */
	public Set<String> getHierarchyNames(HierarchyVertex vertex) {
		return getHierarchy(vertex).stream().map(v -> v.getData().getClassName()).collect(Collectors.toSet());
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Direct descendants of the class.
	 */
	public Stream<String> getDescendants(String name) {
		if (descendents.containsKey(name))
			return descendents.get(name).stream();
		// Empty stream
		return empty();
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return All descendants of the class.
	 */
	public Stream<String> getAllDescendants(String name) {
		Set<String> descendentNames = descendents.get(name);
		if (descendentNames == null)
			return empty();
		return concat(descendentNames.stream(),
				descendentNames.stream().flatMap(this::getAllDescendants));
	}

	/**
	 * @param name
	 * 		Class name.
	 * @param breakCheck
	 * 		Condition to stop scanning for descendants.
	 *
	 * @return All descendants of the class, up until a point specified by the check condition.
	 */
	public Stream<String> getAllDescendantsWithBreakCondition(String name, Predicate<String> breakCheck) {
		Set<String> descendentNames = descendents.get(name);
		if (descendentNames == null)
			return empty();
		descendentNames.removeIf(breakCheck);
		return concat(descendentNames.stream(),
				descendentNames.stream().flatMap(d -> getAllDescendantsWithBreakCondition(d, breakCheck)));
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Direct parents of the class.
	 */
	public Stream<String> getParents(String name) {
		HierarchyVertex vert = getVertex(name);
		if (vert != null)
			return getParents(vert);
		// Empty stream
		return empty();
	}

	/**
	 * @param vertex
	 * 		Class vertex.
	 *
	 * @return Direct parents of the class.
	 */
	public Stream<String> getParents(HierarchyVertex vertex) {
		return concat(
				of(vertex.getData().getSuperName()),
				of(vertex.getData().getInterfaces()));
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return All parents of the class.
	 */
	public Stream<String> getAllParents(String name) {
		return (getParents(name).map(this::getAllParents)
				.reduce(getParents(name), Stream::concat));
	}

	/**
	 * @param first
	 * 		First class name.
	 * @param second
	 * 		Second class name.
	 *
	 * @return Common parent of the classes.
	 */
	public String getCommon(String first, String second) {
		// Full upwards hierarchy for the first
		Set<String> firstParents = getAllParents(first).collect(Collectors.toSet());
		firstParents.add(first);
		// Base case
		if (firstParents.contains(second))
			return second;
		// Iterate over second's parents via breadth-first-search
		Queue<String> queue = new LinkedList<>();
		queue.add(second);
		do {
			// Item to fetch parents of
			String next = queue.poll();
			if (next == null || next.equals("java/lang/Object"))
				break;
			for (String parent : getParents(next).collect(Collectors.toSet())) {
				// Parent in the set of visited classes? Then its valid.
				if(firstParents.contains(parent))
					return parent;
				// Queue up the parent
				if (!parent.equals("java/lang/Object"))
					queue.add(parent);
			}
		} while(!queue.isEmpty());
		// Fallback option
		return "java/lang/Object";
	}

	/**
	 * Check if the given method in a class is linked to a locked library method.
	 *
	 * @param owner
	 * 		Class the method resides in.
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 *
	 * @return {@code true} if any class in the hierarchy of the owner is a library class and
	 * defines the given method,
	 */
	public boolean isLibrary(String owner, String name, String desc) {
		// Get classes that are considered "library" classes (not included in Input)
		Stream<HierarchyVertex> hierarchy = getHierarchy(owner).stream();
		Stream<HierarchyVertex> libClasses = hierarchy.filter(vertex -> !getWorkspace()
				.getPrimaryClassNames().contains(vertex.getClassName()));
		// Check if the library classes have a matching method.
		return libClasses
					.map(ClassVertex::getData)
					.flatMap(cr -> ClassUtil.getMethodDefs(cr).stream())
					.anyMatch(method -> name.equals(method.getKey()) && desc.equals(method.getValue()));
	}

	/**
	 * Check if two methods are linked.
	 * It is assumed that the member definitions <i>(name + desc)</i> exist in their respective owner classes.
	 *
	 * @param owner1
	 * 		First method's defining class.
	 * @param name1
	 * 		First method's name.
	 * @param desc1
	 * 		First method's descriptor.
	 * @param owner2
	 * 		Second method's defining class.
	 * @param name2
	 * 		Second method's name.
	 * @param desc2
	 * 		Second method's descriptor.
	 *
	 * @return {@code true} if the two methods belong to the same hierarchy.
	 */
	public boolean areLinked(String owner1, String name1, String desc1, String owner2, String name2, String desc2) {
		// Obviously mis-matching definitions are not linked
		if (!name1.equals(name2) || !desc1.equals(desc2))
			return false;
		// Check if owner2 is in the same hierarchy as owner1.
		return areLinked(owner1, owner2);
	}

	/**
	 * @param name1
	 * 		Some class name.
	 * @param name2
	 * 		Another class name.
	 *
	 * @return {@code true} if the classes belong to the same hierarchy.
	 */
	public boolean areLinked(String name1, String name2) {
		// Check if name2 is in the same hierarchy as name1.
		return getHierarchy(name1).stream()
				.anyMatch(vertex -> name2.equals(vertex.getClassName()));
	}

	// ============================== UTILITY =================================== //

	/**
	 * Populate {@link #descendents} map.
	 */
	public void refresh() {
		// TODO: Call this when the inheritance tree is modified.
		//  - Already called by mappings
		//  - But later if user changes a class name WITHOUT remappping this needs to be called too
		descendents.clear();
		for (ClassReader reader : getWorkspace().getPrimaryClassReaders()) {
			String superName = reader.getSuperName();
			if (superName == null || !superName.equals("java/lang/Object"))
				descendents.computeIfAbsent(superName, k -> new HashSet<>()).add(reader.getClassName());
			for (String inter : reader.getInterfaces()) {
				descendents.computeIfAbsent(inter, k -> new HashSet<>()).add(reader.getClassName());
			}
		}
	}
}
