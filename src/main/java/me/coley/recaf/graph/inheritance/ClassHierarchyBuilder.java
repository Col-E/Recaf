package me.coley.recaf.graph.inheritance;

import me.coley.recaf.graph.*;
import org.objectweb.asm.ClassReader;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Search implementation builds an inheritance hierarchy when given some class in the hierarchy.
 *
 * @author Matt
 */
public class ClassHierarchyBuilder extends ClassDfsSearch {
	/**
	 * Constructs a class hierarchy builder.
	 */
	public ClassHierarchyBuilder() {
		super(ClassDfsSearch.Type.ALL);
	}

	/**
	 * @param vertex
	 * 		Initial vertex to build from.
	 *
	 * @return Set containing all all vertices with classes in the inheritance hierarchy.
	 */
	public Set<HierarchyVertex> build(Vertex<ClassReader> vertex) {
		// Start a search to populate the visted vertex set.
		// Set the target to some dummy value so the search is exhaustive.
		find(vertex, dummy());
		// Now we have our hierarchy! Additional casting for utility.
		return visited().stream()
				.map(v -> (HierarchyVertex) v)
				.collect(Collectors.toSet());
	}

	/**
	 * @return Dummy vertex used as a target.
	 * Forces an exhaustive search since it can never be matched.
	 */
	private Vertex<ClassReader> dummy() {
		return new HierarchyVertex(null, new ClassReader(DUMMY_CLASS_BYTECODE)) {
			@Override
			public Set<Edge<ClassReader>> getEdges() {
				return Collections.emptySet();
			}

			@Override
			public int hashCode() {
				return -1;
			}

			@Override
			public boolean equals(Object other) {
				return hashCode() == other.hashCode();
			}

			@Override
			public String toString() {
				return "[[Dummy]]";
			}
		};
	}

	/**
	 * The absolute smallest possible dummy class.
	 */
	private static final byte[] DUMMY_CLASS_BYTECODE = new byte[]{ -54, -2, -70, -66, 0, 0, 0, 0,
		0, 5, 1, 0, 1, 97, 7, 0, 1, 1, 0, 1, 98, 7, 0, 3, 0, 0, 0, 2, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0
	};
}
