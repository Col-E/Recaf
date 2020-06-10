package me.coley.recaf.graph;

import me.coley.recaf.util.struct.Pair;

/**
 * Graph edge.
 *
 * @param <T>
 * 		Type of data contained by the graph.
 *
 * @author Matt
 */
public interface Edge<T> {
	/**
	 * @return Pair of verticies in the edge.
	 */
	Pair<Vertex<T>, Vertex<T>> verticies();

	/**
	 * @param notThis
	 * 		One vertex in the pair.
	 *
	 * @return The other vertex of the pair, not the one given.
	 */
	default Vertex<T> getOther(Vertex<T> notThis) {
		Pair<Vertex<T>, Vertex<T>> pair = verticies();
		if(notThis.equals(pair.getKey()))
			return pair.getValue();
		else if(notThis.equals(pair.getValue()))
			return pair.getKey();
		throw new IllegalStateException("Vertex not in pair");
	}
}
