package me.coley.recaf.graph;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @param <V>
 * 		Vertex type.
 * @param <T>
 * 		Vertex value type.
 */
public interface ExhaustiveSearch<V extends Vertex<T>, T> extends Search<T> {
	/**
	 * The absolute smallest possible dummy class.
	 */
	byte[] DUMMY_CLASS_BYTECODE = new byte[]{-54, -2, -70, -66, 0, 0, 0, 52, 0, 5, 1, 0, 1, 97, 7,
		0, 1, 1, 0, 1, 98, 7, 0, 3, 0, 33, 0, 2, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0};

	/**
	 * @param vertex
	 * 		Initial vertex to build from.
	 *
	 * @return Set containing all all vertices with classes in the inheritance hierarchy.
	 */
	@SuppressWarnings("unchecked")
	default Set<V> build(Vertex<T> vertex) {
		// Start a search to populate the visted vertex set.
		// Set the target to some dummy value so the search is exhaustive.
		find(vertex, dummy());
		return visited().stream()
				.map(v -> (V) v)
				.collect(Collectors.toSet());
	}

	/**
	 * @return Dummy vertex used as a target.
	 * Forces an exhaustive search since it can never be matched.
	 */
	Vertex<T> dummy();
}
