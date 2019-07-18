package me.coley.recaf.graph;

import java.util.Set;
import java.util.stream.Stream;

/**
 * Graph vertex.
 *
 * @param <T>
 * 		Type of data contained by the graph.
 *
 * @author Matt
 */
public abstract class Vertex<T> {
	/**
	 * @return Contained data.
	 */
	public abstract T getData();

	/**
	 * @param data
	 * 		New data to contain.
	 */
	public abstract void setData(T data);

	/**
	 * @return Hash of {@link #getData() data}.
	 */
	public abstract int hashCode();

	/**
	 * @param other
	 * 		Other item to check.
	 *
	 * @return {@code true} if this vertex's {@link #getData() data} matches the given object.
	 */
	public abstract boolean equals(Object other);

	/**
	 * @return Collection of edges connected to this vertex.
	 */
	public abstract Set<Edge<T>> getEdges();

	/**
	 * @return Edges that are not directed.
	 */
	public Stream<Edge<T>> getUndirectedEdges() {
		return getEdges().stream()
				.filter(e -> !(e instanceof DirectedEdge));
	}

	/**
	 * @param isParent
	 * 		Flag for if the current vertex is the parent in the directed edge relation.
	 *
	 * @return Edges where the current vertex is a parent or is a child depending on the given flag.
	 */
	public Stream<DirectedEdge<T>> getDirectedEdges(boolean isParent) {
		return getEdges().stream()
				.filter(e -> e instanceof DirectedEdge)
				.map(e -> (DirectedEdge<T>) e)
				.filter(di -> isParent ? equals(di.getParent()) : equals(di.getChild()));
	}

	/**
	 * @return Vertices that are direct descendants of this vertex in a directed graph.
	 */
	public Stream<Vertex<T>> getDirectedChildren() {
		return getDirectedEdges(true)
				.map(e -> e.getChild());
	}

	/**
	 * @return Vertices that are direct descendants of this vertex in a directed graph.
	 */
	public Stream<Vertex<T>> getDirectedParents() {
		return getDirectedEdges(false)
				.map(e -> e.getParent());
	}

	/**
	 * @param includeSelf
	 * 		Flag for if the current vertex is to be included in the results.
	 *
	 * @return Vertices that are descendants of this vertex in a directed graph.
	 */
	public Stream<Vertex<T>> getAllDirectedChildren(boolean includeSelf) {
		return includeSelf ?
				Stream.concat(Stream.of(this), getAllDirectedChildren()) : getAllDirectedChildren();
	}

	/**
	 * @return Vertices that are descendants of this vertex in a directed graph.
	 */
	private Stream<Vertex<T>> getAllDirectedChildren() {
		// Reduce merges the current vertices' directed children with the master stream.
		return (getDirectedChildren().map(x -> x.getAllDirectedChildren())
				.reduce(getDirectedChildren(), (master, children) -> Stream.concat(master, children)));
	}


	/**
	 * @param includeSelf
	 * 		Flag for if the current vertex is to be included in the results.
	 *
	 * @return Vertices that this vertex inherits from in a directed graph.
	 */
	public Stream<Vertex<T>> getAllDirectedParents(boolean includeSelf) {
		return includeSelf ?
				Stream.concat(Stream.of(this), getAllDirectedParents()) : getAllDirectedParents();
	}

	/**
	 * @return Vertices that this vertex inherits from in a directed graph.
	 */
	private Stream<Vertex<T>> getAllDirectedParents() {
		// Reduce merges the current vertices' directed parents with the master stream.
		return (getDirectedParents().map(x -> x.getAllDirectedParents())
				.reduce(getDirectedParents(), (master, parent) -> Stream.concat(master, parent)));
	}

	/**
	 * @param isParent
	 * 		Flag for if the current vertex is the parent in directed edge relations.
	 *
	 * @return Edges where the relation is undirected, or if directed that the current vertex is a
	 * parent or is a child depending on the given flag.
	 */
	public Stream<Edge<T>> getApplicableEdges(boolean isParent) {
		return Stream.concat(getUndirectedEdges(), getDirectedEdges(isParent));
	}
}
