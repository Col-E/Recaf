package me.coley.recaf.bytecode.analysis;

import me.coley.recaf.graph.*;
import org.objectweb.asm.tree.ClassNode;

import java.util.stream.Stream;

/**
 * Search implmentation that does not iterate over any edges originating from the "Object" class.
 *
 * @author Matt
 */
public class ClassDfsSearch extends DepthFirstSearch<ClassNode> {
	private final Type edgeType;

	/**
	 * @param edgeType
	 * 		Kind of edge traversal to use. Options are:
	 * 		<ul>
	 * 		<li><b>Children</b> - For checking if the root is a parent of the target vertex.</li>
	 * 		<li><b>Parents</b> - For checking if the root is a child of the target vertex.</li>
	 * 		<li><b>All</b> - For scanning the entire class hierarchy <i>(Classes connected via
	 * 		inheritence of classes excluding "Object")</i> regardless of inheritence direction
	 * 		.</li>
	 * 		</ul>
	 */
	public ClassDfsSearch(Type edgeType) {
		this.edgeType = edgeType;
	}

	@Override
	protected Stream<Edge<ClassNode>> edges(Vertex<ClassNode> root) {
		// We do NOT want ANY edges pointing away from Object.
		if (root.toString().equals("java/lang/Object")){
			return Stream.of();
		}
		switch(edgeType) {
			case CHILDREN:
				return root.getApplicableEdges(true);
			case PARENTS:
				return root.getApplicableEdges(false);
			case ALL:
			default:
				return root.getEdges().stream();
		}
	}

	public enum Type {
		CHILDREN, PARENTS, ALL
	}
}
