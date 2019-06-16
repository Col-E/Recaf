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
public interface Graph<T, V extends Vertex<T>> {
	/**
	 * @return Set of entry points to the graph.
	 */
	 Set<V> roots();

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
	default Map<T, V> rootMap() {
		return roots().stream()
				.collect(Collectors.toMap(v -> v.getData(), v -> v));
	}

	/**
	 * @param key
	 * 		Some key.
	 *
	 * @return A root instance associated with it's contained data.
	 */
	default V getRoot(T key) {
		return rootMap().get(key);
	}

	/**
	 * An alternative to {@link #getRoot(Object)} which does not use the generative method
	 * {@link #rootMap()}. This allows for much faster lookup-times.
	 * <br>
	 * By default this is not implemented. It may optionally be implemented by children.
	 *
	 * @param key
	 * 		Some key.
	 *
	 * @return A root instance associated with it's contained data.
	 */
	default V getRootFast(T key) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @param vertex
	 * 		Vertex to check for.
	 *
	 * @return {@code true} if root set contains the given vertex.
	 */
	default boolean containsRoot(V vertex) {
		return roots().contains(vertex);
	}

	/**
	 * @param key
	 * 		Key value to check for.
	 *
	 * @return {@code true} if root set contains the given value.
	 */
	default boolean containsRoot(T key) {
		return rootValues().contains(key);
	}
}
