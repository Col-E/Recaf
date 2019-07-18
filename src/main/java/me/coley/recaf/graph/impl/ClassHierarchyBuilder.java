package me.coley.recaf.graph.impl;

import me.coley.recaf.graph.Edge;
import me.coley.recaf.graph.Vertex;
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
	public ClassHierarchyBuilder() {
		super(ClassDfsSearch.Type.ALL);
	}

	public Set<ClassVertex> build(Vertex<ClassReader> root) {
		// Start a search to populate the visted vertex set.
		// Set the target to some dummy value so the search is exhaustive.
		find(root, dummy());
		// Now we have our hierarchy! Additional casting for utility.
		return visited().stream().map(v -> (ClassVertex) v)
				.collect(Collectors.toSet());
	}

	/**
	 * @return Dummy vertex used as a target.
	 * Forces an exhaustive search since it can never be matched.
	 */
	private Vertex<ClassReader> dummy() {
		return new ClassVertex(null, new ClassReader(DUMMY_CLASS_BYTECODE)) {
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
	private final static byte[] DUMMY_CLASS_BYTECODE = new byte[]{-54, -2, -70, -66, 0, 0, 0, 0,
			0, 5, 1, 0, 1, 97, 7, 0, 1, 1, 0, 1, 98, 7, 0, 3, 0, 0, 0, 2, 0, 4, 0, 0, 0, 0, 0, 0,
			0, 0};
}
