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
	private final boolean ignoreEdgeDirections;

	/**
	 * @param ignoreEdgeDirections
	 * 		Used in cases where we want to find the <i>ENTIRE</i> hierarchy of a class family.
	 * 		This is useful for generating a set of classes that can potentially share a method
	 * 		definition.<br>
	 * 		If false, then the result will only include children of this class.
	 */
	public ClassDfsSearch(boolean ignoreEdgeDirections) {
		this.ignoreEdgeDirections = ignoreEdgeDirections;
	}

	@Override
	protected Stream<Edge<ClassNode>> edges(Vertex<ClassNode> root) {
		// We do NOT want ANY edges pointing away from Object.
		if (root.toString().equals("java/lang/Object")){
			return Stream.of();
		}
		// If directions don't matter, stream ALL edges.
		if (ignoreEdgeDirections) {
			return root.getEdges().stream();
		}
		// Standard edge return value, where the "root" vertex is the parent vertex of a relation.
		return root.getApplicableEdges(true);
	}
}
