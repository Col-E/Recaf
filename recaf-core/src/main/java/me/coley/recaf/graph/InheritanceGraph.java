package me.coley.recaf.graph;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.ClassInfo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class inheritance graph utility.
 *
 * @author Matt Coley
 */
public class InheritanceGraph {
	private final Multimap<String, String> parentToChild = MultimapBuilder.hashKeys().hashSetValues().build();
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

	// TODO: "refreshChildLookup" needs to be called whenever:
	//  - Class is renamed
	//  - Class is added
	//  - Class hierarchy is modified

	/**
	 * Refresh parent-to-child lookup.
	 */
	public void refreshChildLookup() {
		parentToChild.clear();
		for (ClassInfo info : workspace.getResources().getClasses()) {
			parentToChild.put(info.getSuperName(), info.getName());
			for (String itf : info.getInterfaces())
				parentToChild.put(itf, info.getName());
		}
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
		ClassInfo info = workspace.getResources().getClass(name);
		if (info == null)
			return null;
		boolean isPrimary = workspace.getResources().getPrimary().getClasses().containsKey(info.getName());
		return new InheritanceVertex(info, this::getVertex, this::getDirectChildren, isPrimary);
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
		Set<String> firstParents = getVertex(first).getAllParents().stream()
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
			if (next == null || next.equals("java/lang/Object"))
				break;
			for (String parent : getVertex(next).getParents().stream()
					.map(InheritanceVertex::getName).collect(Collectors.toSet())) {
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
}