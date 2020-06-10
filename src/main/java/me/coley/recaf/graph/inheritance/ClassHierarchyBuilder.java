package me.coley.recaf.graph.inheritance;

import me.coley.recaf.graph.*;
import org.objectweb.asm.ClassReader;

import java.util.Collections;
import java.util.Set;

/**
 * Search implementation that builds an inheritance hierarchy when given some class in the hierarchy.
 *
 * @author Matt
 */
public class ClassHierarchyBuilder extends ClassDfsSearch implements ExhaustiveSearch<HierarchyVertex, ClassReader> {
	/**
	 * Constructs a class hierarchy builder.
	 */
	public ClassHierarchyBuilder() {
		this(ClassDfsSearch.Type.ALL);
	}

	/**
	 * Constructs a class hierarchy builder.
	 *
	 * @param type
	 * 		Allowed edge type.
	 */
	public ClassHierarchyBuilder(ClassDfsSearch.Type type) {
		super(type);
	}

	@Override
	public Vertex<ClassReader> dummy() {
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
				if(other instanceof HierarchyVertex)
					return hashCode() == other.hashCode();
				return this == other;
			}

			@Override
			public String toString() {
				return "[[Dummy]]";
			}
		};
	}
}
