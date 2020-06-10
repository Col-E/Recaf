package me.coley.recaf.graph;

import me.coley.recaf.util.struct.Pair;

/**
 * Graph directed-edge.
 * <pre>
 *     Parent -&gt; Child.
 * </pre>
 *
 * @param <T>
 * 		Type of data contained by the graph.
 *
 * @author Matt
 */
public class DirectedEdge<T> implements Edge<T> {
	private final Vertex<T> parent;
	private final Vertex<T> child;

	/**
	 * Constructs a directed edge.
	 *
	 * @param parent
	 * 		The parent vertex of the edge.
	 * @param child
	 * 		The child vertex of the edge.
	 */
	public DirectedEdge(Vertex<T> parent, Vertex<T> child) {
		this.parent = parent;
		this.child = child;
	}

	/**
	 * @return Pair of the vertices.
	 */
	public Pair<Vertex<T>, Vertex<T>> verticies() {
		return new Pair<>(getParent(), getChild());
	}

	/**
	 * @return Parent vertex.
	 */
	public Vertex<T> getParent() {
		return parent;
	}

	/**
	 * @return Child vertex.
	 */
	public Vertex<T> getChild() {
		return child;
	}
}
