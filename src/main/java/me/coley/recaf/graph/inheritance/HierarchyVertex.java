package me.coley.recaf.graph.inheritance;

import me.coley.recaf.graph.*;
import org.objectweb.asm.ClassReader;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ClassVertex implementation with the inheritance tree used as the edge source.
 *
 * @author Matt
 */
public class HierarchyVertex extends ClassVertex<HierarchyGraph> {
	/**
	 * Constructs a hierarchy vertex from the containing hierarchy and class reader.
	 *
	 * @param graph
	 * 		The containing hierarchy.
	 * @param clazz
	 * 		The vertex data.
	 */
	public HierarchyVertex(HierarchyGraph graph, ClassReader clazz) {
		super(graph, clazz);
	}

	@Override
	public Set<Edge<ClassReader>> getEdges() {
		// Get names of parents/children
		Stream<String> parents = graph.getParents(getData().getClassName());
		Stream<String> children = graph.getDescendants(getData().getClassName());
		// Get values of parents/children
		Stream<ClassReader> parentValues = getReadersFromNames(parents);
		Stream<ClassReader> childrenValues = getReadersFromNames(children);
		// Get edges of parents/children
		Stream<Edge<ClassReader>> parentEdges = parentValues.map(node -> {
			HierarchyVertex other = graph.getVertex(node.getClassName());
			if(other == null) {
				other = new HierarchyVertex(graph, node);
			}
			return new DirectedEdge<>(other, HierarchyVertex.this);
		});
		Stream<Edge<ClassReader>> childrenEdges = childrenValues.map(node -> {
			HierarchyVertex other = graph.getVertex(node.getClassName());
			return new DirectedEdge<>(HierarchyVertex.this, other);
		});
		// Concat edges and return as set.
		return Stream.concat(parentEdges, childrenEdges).collect(Collectors.toSet());
	}
}
