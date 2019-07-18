package me.coley.recaf.graph;

import java.util.List;

/**
 * Search result data.
 *
 * @param <T>
 * 		Type of data contained by the graph.
 *
 * @author Matt
 */
public class SearchResult<T> {
	private final List<Vertex<T>> path;

	public SearchResult(List<Vertex<T>> path) {
		this.path = path;
	}

	/**
	 * @return Path taken to get from the root node to the target.
	 */
	public List<Vertex<T>> getPath() {
		return path;
	}
}
