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
				.map(DirectedEdge::getChild);
	}

	/**
	 * @return Vertices that are direct descendants of this vertex in a directed graph.
	 */
	public Stream<Vertex<T>> getDirectedParents() {
		return getDirectedEdges(false)
				.map(DirectedEdge::getParent);
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
		return Stream.concat(getDirectedChildren(),
				getDirectedChildren().flatMap(Vertex::getAllDirectedChildren));
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
		return Stream.concat(getDirectedParents(),
				getDirectedParents().flatMap(Vertex::getAllDirectedParents));
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

	/**
	 * @return {@code true} if the vertex has no directed parent edges.
	 */
	public boolean isRoot() {
		return getDirectedParents().count() == 0;
	}

	/**
	 * @return {@code true} if the vertex has no directed children edges.
	 */
	public boolean isLeaf() {
		return getDirectedChildren().count() == 0;
	}

	/**
	 * @return {@code this} if the current vertex is a root. Otherwise a set of roots in the graph
	 * this vertex resides in.
	 */
	public Stream<Vertex<T>> getAllRoots() {
		return getAllDirectedParents(true).filter(Vertex::isRoot);
	}

	/**
	 * @return {@code this} if the current vertex is a leaf. Otherwise a set of leaves in the graph
	 * this vertex resides in.
	 */
	public Stream<Vertex<T>> getAllLeaves() {
		return getAllDirectedChildren(true).filter(Vertex::isLeaf);
	}
}
