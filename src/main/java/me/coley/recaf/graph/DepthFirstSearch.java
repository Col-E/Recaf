package me.coley.recaf.graph;

import java.util.*;
import java.util.stream.Stream;

/**
 * Depth-first search implementation for traversing a graph.
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
		if (vertex == null)
			throw new IllegalArgumentException("Cannot search with a null initial vertex!");
		if (target == null)
			throw new IllegalArgumentException("Cannot search with a null target vertex!");
		// Skip already visited vertices
		if(shouldSkip(vertex))
			return null;
		// Mark as visited
		onVisit(path, vertex);
		// Update path
		path.add(vertex);
		// Check for match
		if(vertex.equals(target))
			return createResult(path);
		// Iterate over edges
		Optional<SearchResult<T>> res = edges(vertex)
				.map(edge -> find(edge.getOther(vertex), target, new ArrayList<>(path)))
				.filter(Objects::nonNull)
				.findFirst();
		// Result found?
		return res.orElse(null);
	}

	/**
	 * @param vertex
	 * 		Analyzed vertex.
	 *
	 * @return {@code true} if the search should continue using the given vertex's edges. {@code
	 * false} to ignore this vertex's edges.
	 */
	protected boolean shouldSkip(Vertex<T> vertex) {
		return visited().contains(vertex);
	}

	/**
	 * Called when the given vertex is visited.
	 *
	 * @param currentPath
	 * 		Current path.
	 * @param vertex
	 * 		Vertex visted.
	 */
	protected void onVisit(List<Vertex<T>> currentPath, Vertex<T> vertex) {
		visited().add(vertex);
	}

	/**
	 * @param vertex
	 * 		Vertex to fetch edges from.
	 *
	 * @return Directed edges of the given vertex where the vertex is the parent.
	 */
	protected Stream<Edge<T>> edges(Vertex<T> vertex) {
		return vertex.getApplicableEdges(true);
	}

	/**
	 * @param path
	 * 		Path visited to complete the search.
	 *
	 * @return Result wrapper of the path.
	 */
	protected SearchResult<T> createResult(List<Vertex<T>> path) {
		return new SearchResult<>(path);
	}
}
