package me.coley.recaf.graph;

import java.util.*;
import java.util.stream.Stream;

/**
 * Depth-first search implmentation for traversing a graph.
 *
 * @param <T>
 * 		Type of data contained by the graph.
 *
 * @author Matt
 */
public class DepthFirstSearch<T> implements Search<T> {
	private final Set<Vertex<T>> visted = new HashSet<>();

	@Override
	public Set<Vertex<T>> visited() {
		return visted;
	}

	@Override
	public SearchResult<T> find(Vertex<T> root, Vertex<T> target) {
		return find(root, target, new ArrayList<>());
	}

	private SearchResult<T> find(Vertex<T> root, Vertex<T> target, List<Vertex<T>> path) {
		// Verify parameters
		if (root == null || target == null)
			return null;
		// Skip already visited vertices
		if(visited().contains(root))
			return null;
		visited().add(root);
		// Update path
		path.add(root);
		// Check for match
		if(root.equals(target))
			return createResult(path);
		// Iterate over edges
		Optional<SearchResult<T>> res = edges(root)
				.map(edge -> find(edge.getOther(root), target, new ArrayList<>(path)))
				.filter(result -> result != null)
				.findFirst();
		// Result found?
		if (res.isPresent())
			return res.get();
		return null;
	}

	protected Stream<Edge<T>> edges(Vertex<T> root) {
		return root.getApplicableEdges(true);
	}

	protected SearchResult createResult(List<Vertex<T>> path) {
		return new SearchResult(path);
	}
}
