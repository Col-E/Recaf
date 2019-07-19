package me.coley.recaf.graph.inheritance;

import me.coley.recaf.graph.*;
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
		// We do NOT want ANY edges pointing away from Object.
		if (vertex.toString().equals("java/lang/Object")){
			return Stream.of();
		}
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

	public enum Type {
		CHILDREN, PARENTS, ALL
	}
}
