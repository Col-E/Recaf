package me.coley.recaf.graph;

import org.objectweb.asm.ClassReader;

import java.util.stream.Stream;

/**
 * Search implementation that does not iterate over any edges originating from the "Object" class.
 *
 * @author Matt
 */
public class ClassDfsSearch extends DepthFirstSearch<ClassReader> {
	private final Type edgeType;

	/**
	 * @param edgeType
	 * 		Kind of edge traversal to use. Options are:
	 * 		<ul>
	 * 		<li><b>Children</b> - For checking if the vertex is a parent of the target vertex.</li>
	 * 		<li><b>Parents</b> - For checking if the vertex is a child of the target vertex.</li>
	 * 		<li><b>All</b> - For scanning the entire class hierarchy <i>(Classes connected via
	 * 		inheritence of classes excluding "Object")</i> regardless of inheritence direction
	 * 		.</li>
	 * 		</ul>
	 */
	public ClassDfsSearch(Type edgeType) {
		this.edgeType = edgeType;
	}

	@Override
	protected Stream<Edge<ClassReader>> edges(Vertex<ClassReader> vertex) {
		if (cancel(vertex))
			return Stream.empty();
		switch(edgeType) {
			case CHILDREN:
				return vertex.getApplicableEdges(true);
			case PARENTS:
				return vertex.getApplicableEdges(false);
			case ALL:
			default:
				return vertex.getEdges().stream();
		}
	}

	/**
	 * @param vertex
	 * 		Vertex being looked at in the search.
	 *
	 * @return {@code true} if the search should terminate here. {@code false} to allow iteration
	 * of this vertex's edges.
	 */
	protected boolean cancel(Vertex<ClassReader> vertex) {
		// We do NOT want ANY edges pointing away from Object.
		return vertex.toString().equals("java/lang/Object");
	}

	/**
	 * Type of edges to visit in search.
	 */
	public enum Type {
		CHILDREN, PARENTS, ALL
	}
}
