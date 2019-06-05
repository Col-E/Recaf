package me.coley.recaf.graph;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base graph, intended for creation of <i>generative</i> graphs <i>(Where vertices and edges are
 * dynamically created/requested)</i>.
 *
 * @param <T>
 * 		Type of data contained by the graph.
 *
 * @author Matt
 */
public interface Graph<T> {
	/**
	 * @return Set of entry points to the graph.
	 */
	Set<Vertex<T>> roots();

	/**
	 * @return Set of values contained by the {@link #roots()}.
	 */
	default Set<T> rootValues() {
		return roots().stream()
				.map(v -> v.getData())
				.collect(Collectors.toSet());
	}

	/**
	 * @return A map of {@link #rootValues() root values} to their {@link #roots() vertices}.
	 */
	default Map<T, Vertex<T>> rootMap() {
		return roots().stream()
				.collect(Collectors.toMap(v -> v.getData(), v -> v));
	}

	/**
	 * @param value
	 * 		Some value.
	 * @param <V>
	 * 		Type of extended vertex class.
	 *
	 * @return A root instance associated with it's contained data.
	 */
	default <V extends Vertex<T>> V getRoot(T value) {
		return (V) rootMap().get(value);
	}

	/**
	 * @param vertex
	 * 		Vertex to check for.
	 *
	 * @return {@code true} if root set contains the given vertex.
	 */
	default boolean containsRoot(Vertex<T> vertex) {
		return roots().contains(vertex);
	}

	/**
	 * @param value
	 * 		Value to check for.
	 *
	 * @return {@code true} if root set contains the given value.
	 */
	default boolean containsRoot(T value) {
		return rootValues().contains(value);
	}
}
