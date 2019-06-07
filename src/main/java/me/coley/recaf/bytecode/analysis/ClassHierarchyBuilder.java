package me.coley.recaf.bytecode.analysis;

import me.coley.recaf.graph.Edge;
import me.coley.recaf.graph.Vertex;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Search implmentation builds an inheritence hierarchy when given some class in the hierarchy.
 *
 * @author Matt
 */
public class ClassHierarchyBuilder extends ClassDfsSearch {
	public ClassHierarchyBuilder() {
		super(ClassDfsSearch.Type.ALL);
	}

	public Set<ClassVertex> build(Vertex<ClassNode> root) {
		// Start a search to populate the visted vertex set.
		// Set the target to some dummy value so the search is exhaustive.
		find(root, dummy());
		// Now we have our hierarchy! Additional casting for utility.
		return visited().stream().map(v -> (ClassVertex) v)
				.collect(Collectors.toSet());
	}

	/**
	 * @return Dummy vertex used as a target.
	 * Forces an exhaustive search since it can never be matched.
	 */
	private Vertex<ClassNode> dummy() {
		return new ClassVertex(null, new ClassNode()) {
			@Override
			public Set<Edge<ClassNode>> getEdges() {
				return Collections.emptySet();
			}
		};
	}
}
