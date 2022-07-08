package me.coley.recaf.graph;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resources;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Class inheritance graph utility.
 *
 * @author Matt Coley
 */
public class InheritanceGraph {
	private static final InheritanceVertex STUB = new InheritanceVertex(null, null, null, false);
	private static final String OBJECT = "java/lang/Object";
	private final Multimap<String, String> parentToChild = MultimapBuilder.hashKeys().hashSetValues().build();
	private final Map<String, InheritanceVertex> vertices = new ConcurrentHashMap<>();
	private final Function<String, InheritanceVertex> vertexProvider = createVertexProvider();
	private final Workspace workspace;

	/**
	 * Create an inheritance graph.
	 *
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	public InheritanceGraph(Workspace workspace) {
		this.workspace = workspace;
		refreshChildLookup();
	}

	/**
	 * Refresh parent-to-child lookup.
	 */
	public void refreshChildLookup() {
		// Clear
		parentToChild.clear();
		// Repopulate
		workspace.getResources().getClasses()
				.forEach(this::populateParentToChildLookup);
	}

	/**
	 * Populate a references from the given child class to the parent class.
	 *
	 * @param name
	 * 		Child class name.
	 * @param parentName
	 * 		Parent class name.
	 */
	public void populateParentToChildLookup(String name, String parentName) {
		parentToChild.put(parentName, name);
	}

	/**
	 * Populate all references from the given child class to its parents.
	 *
	 * @param info
	 * 		Child class.
	 */
	public void populateParentToChildLookup(CommonClassInfo info) {
		populateParentToChildLookup(info.getName(), info.getSuperName());
		for (String itf : info.getInterfaces())
			populateParentToChildLookup(info.getName(), itf);
	}

	/**
	 * Remove all references from the given child class to its parents.
	 *
	 * @param info
	 * 		Child class.
	 */
	public void removeParentToChildLookup(CommonClassInfo info) {
		removeParentToChildLookup(info.getName(), info.getSuperName());
		for (String itf : info.getInterfaces())
			removeParentToChildLookup(info.getName(), itf);
	}

	/**
	 * Remove a references from the given child class to the parent class.
	 *
	 * @param name
	 * 		Child class name.
	 * @param parentName
	 * 		Parent class name.
	 */
	public void removeParentToChildLookup(String name, String parentName) {
		parentToChild.remove(parentName, name);
	}

	/**
	 * @param parent
	 * 		Parent to find children of.
	 *
	 * @return Direct extensions/implementations of the given parent.
	 */
	private Collection<String> getDirectChildren(String parent) {
		return parentToChild.get(parent);
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Vertex in graph of class. {@code null} if no such class was found in the inputs.
	 */
	public InheritanceVertex getVertex(String name) {
		InheritanceVertex vertex = vertices.computeIfAbsent(name, vertexProvider);
		return vertex == STUB ? null : vertex;
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Complete inheritance family of the class.
	 */
	public Set<InheritanceVertex> getVertexFamily(String name) {
		InheritanceVertex vertex = getVertex(name);
		if (vertex == null)
			return Collections.emptySet();
		return vertex.getFamily();
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
		InheritanceVertex vertex = getVertex(first);
		if (vertex == null || OBJECT.equals(first) || OBJECT.equals(second))
			return OBJECT;
		Set<String> firstParents = getVertex(first).parents()
				.map(InheritanceVertex::getName).collect(Collectors.toSet());
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
			if (next == null || next.equals(OBJECT))
				break;
			InheritanceVertex nextVertex = getVertex(next);
			if (nextVertex == null)
				break;
			for (String parent : nextVertex.getParents().stream()
					.map(InheritanceVertex::getName).collect(Collectors.toSet())) {
				// Parent in the set of visited classes? Then its valid.
				if (firstParents.contains(parent))
					return parent;
				// Queue up the parent
				if (!parent.equals(OBJECT))
					queue.add(parent);
			}
		} while (!queue.isEmpty());
		// Fallback option
		return OBJECT;
	}

	private Function<String, InheritanceVertex> createVertexProvider() {
		return name -> {
			Resources resources = workspace.getResources();
			CommonClassInfo info = resources.getClass(name);
			if (info == null)
				info = resources.getDexClass(name);
			if (info == null)
				return STUB;
			boolean isPrimary = resources.getPrimary().getClasses().containsKey(name) ||
					resources.getPrimary().getDexClasses().containsKey(name);
			return new InheritanceVertex(info, this::getVertex, this::getDirectChildren, isPrimary);
		};
	}
}