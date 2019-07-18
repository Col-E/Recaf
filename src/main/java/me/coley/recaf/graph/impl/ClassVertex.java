package me.coley.recaf.graph.impl;

import me.coley.recaf.graph.*;
import me.coley.recaf.util.Classpath;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Graph vertex with a {@link org.objectweb.asm.ClassReader} as the data.
 *
 * @author Matt
 */
public class ClassVertex extends Vertex<ClassReader> {
	private final Hierarchy graph;
	private ClassReader clazz;

	public ClassVertex(Hierarchy graph, ClassReader clazz) {
		this.clazz = clazz;
		this.graph = graph;
	}

	@Override
	public ClassReader getData() {
		return clazz;
	}

	@Override
	public void setData(ClassReader clazz) {
		this.clazz = clazz;
	}

	@Override
	public int hashCode() {
		return getData().getClassName().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if(other == null)
			throw new IllegalStateException();
		if(this == other)
			return true;
		if(other instanceof ClassVertex) {
			ClassVertex otherVertex = (ClassVertex) other;
			if(getData().getClassName().equals(otherVertex.getData().getClassName()))
				return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return getData().getClassName();
	}

	@Override
	public Set<Edge<ClassReader>> getEdges() {
		// Get names of parents/children
		Stream<String> parents = graph.getParents(getData().getClassName());
		Stream<String> children = graph.getDescendants(getData().getClassName());
		// Get values of parents/children
		Stream<ClassReader> parentValues = getNodesFromNames(parents);
		Stream<ClassReader> childrenValues = getNodesFromNames(children);
		// Get edges of parents/children
		Stream<Edge<ClassReader>> parentEdges = parentValues.map(node -> {
			ClassVertex other = graph.getRoot(node.getClassName());
			if(other == null) {
				other = new ClassVertex(graph, node);
			}
			return new DirectedEdge<>(other, ClassVertex.this);
		});
		Stream<Edge<ClassReader>> childrenEdges = childrenValues.map(node -> {
			ClassVertex other = graph.getRoot(node.getClassName());
			return new DirectedEdge<>(ClassVertex.this, other);
		});
		// Concat edges and return as set.
		return Stream.concat(parentEdges, childrenEdges).collect(Collectors.toSet());
	}

	// ============================== UTILITY =================================== //

	/**
	 * @param names
	 * 		Stream of names of classes.
	 *
	 * @return Mapped stream where names are replaced with instances.
	 * If a name has no instance mapping, it is discarded.
	 */
	private Stream<ClassReader> getNodesFromNames(Stream<String> names) {
		return names.map(name -> {
			// Try loading from workspace
			ClassReader reader = graph.getWorkspace().getClassReader(name);
			if(reader != null)
				return reader;
			// Try loading from runtime
			return fromRuntime(name);
		}).filter(node -> node != null);
	}

	/**
	 * @param name
	 * 		Internal class name.
	 *
	 * @return {@link org.objectweb.asm.ClassReader} loaded from runtime.
	 */
	private static ClassReader fromRuntime(String name) {
		try {
			Class<?> loaded = Classpath.getSystemClass(normalize(name));
			return new ClassReader(loaded.getName());
		} catch(ClassNotFoundException | IOException e) {
			// Expected / allowed: ignore these
		} catch(Exception e) {
			// Log other unexpected exceptions
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param internal
	 * 		Internal class name.
	 *
	 * @return Standard class name.
	 */
	private static String normalize(String internal) {
		return internal.replace("/", ".").replace("$", ".");
	}
}
