package me.coley.recaf.graph;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import static java.util.function.Function.identity;

/**
 * Base graph, intended for creation of <i>generative</i> graphs <i>(Where vertices and edges are
 * dynamically created/requested)</i>.
 *
 * @param <T>
 * 		Type of data contained by the graph.
 * @param <V>
 * 		Vertex class type.
 *
 * @author Matt
 */
public interface Graph<T, V extends Vertex<T>> {
	/**
	 * @return Set of vertices in the graph.
	 */
	Set<V> vertices();

	/**
	 * @return Set of values contained by the {@link #vertices()}.
	 */
	default Set<T> verticesValues() {
		return vertices().stream()
				.map(Vertex::getData)
				.collect(Collectors.toSet());
	}

	/**
	 * @return A map of {@link #verticesValues() vertex values} to their
	 * {@link #vertices() vertices}.
	 */
	default Map<T, V> verticesMap() {
		return vertices().stream()
				.collect(Collectors.toMap(Vertex::getData, identity()));
	}

	/**
	 * @param key
	 * 		Some key.
	 *
	 * @return A vertex instance associated with it's contained data.
	 */
	default V getVertex(T key) {
		return verticesMap().get(key);
	}

	/**
	 * An alternative to {@link #getVertex(Object)} which does not use the generative method
	 * {@link #verticesMap()}. This allows for much faster lookup-times.
	 * <br>
	 * By default this is not implemented. It may optionally be implemented by children.
	 *
	 * @param key
	 * 		Some key.
	 *
	 * @return A vertex instance associated with it's contained data.
	 */
	default V getVertexFast(T key) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @param vertex
	 * 		Vertex to check for.
	 *
	 * @return {@code true} if the {@link #vertices() vertex set} contains the given vertex.
	 */
	default boolean containsVertex(V vertex) {
		return vertices().contains(vertex);
	}

	/**
	 * @param key
	 * 		Key value to check for.
	 *
	 * @return {@code true} if the {@link #vertices() vertex set} contains the given value.
	 */
	default boolean containsVertex(T key) {
		return verticesValues().contains(key);
	}
}
