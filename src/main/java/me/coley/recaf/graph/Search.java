package me.coley.recaf.graph;

import java.util.Set;

/**
 * Base search layout.
 *
 * @param <T>
 * 		Type of data contained by the graph.
 *
 * @author Matt
 */
public interface Search<T> {
	/**
	 * @return Set of visited verticies.
	 */
	Set<Vertex<T>> visited();

	/**
	 * @param vertex
	 * 		Vertex currently in-use for the search.
	 * @param target
	 * 		Intended target to find.
	 *
	 * @return Result summarizing how the vertices are related. If there is no relation returns
	 * {@code null}.
	 */
	SearchResult<T> find(Vertex<T> vertex, Vertex<T> target);
}
