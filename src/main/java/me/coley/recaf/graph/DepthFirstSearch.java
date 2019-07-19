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
	public SearchResult<T> find(Vertex<T> vertex, Vertex<T> target) {
		return find(vertex, target, new ArrayList<>());
	}

	private SearchResult<T> find(Vertex<T> vertex, Vertex<T> target, List<Vertex<T>> path) {
		// Verify parameters
		if (vertex == null || target == null)
			return null;
		// Skip already visited vertices
		if(visited().contains(vertex))
			return null;
		visited().add(vertex);
		// Update path
		path.add(vertex);
		// Check for match
		if(vertex.equals(target))
			return createResult(path);
		// Iterate over edges
		Optional<SearchResult<T>> res = edges(vertex)
				.map(edge -> find(edge.getOther(vertex), target, new ArrayList<>(path)))
				.filter(result -> result != null)
				.findFirst();
		// Result found?
		if (res.isPresent())
			return res.get();
		return null;
	}

	protected Stream<Edge<T>> edges(Vertex<T> vertex) {
		return vertex.getApplicableEdges(true);
	}

	protected SearchResult createResult(List<Vertex<T>> path) {
		return new SearchResult(path);
	}
}
