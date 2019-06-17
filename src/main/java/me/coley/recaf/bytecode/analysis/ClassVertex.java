package me.coley.recaf.bytecode.analysis;

import me.coley.recaf.bytecode.ClassUtil;
import me.coley.recaf.graph.*;
import me.coley.recaf.util.Classpath;

import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassVertex extends Vertex<ClassNode> {
	private final Hierarchy graph;
	private ClassNode clazz;

	public ClassVertex(Hierarchy graph, ClassNode clazz) {
		this.clazz = clazz;
		this.graph = graph;
	}

	@Override
	public ClassNode getData() {
		return clazz;
	}

	@Override
	public void setData(ClassNode clazz) {
		this.clazz = clazz;
	}

	@Override
	public int hashCode() {
		return getData().name.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			throw new IllegalStateException();
		if (this == other)
			return true;
		if(other instanceof ClassVertex) {
			ClassVertex otherVertex = (ClassVertex) other;
			if(getData().name.equals(otherVertex.getData().name))
				return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return getData().name;
	}

	@Override
	public Set<Edge<ClassNode>> getEdges() {
		// Get names of parents/children
		Stream<String> parents = graph.getParents(getData().name);
		Stream<String> children = graph.getDescendants(getData().name);
		// Get values of parents/children
		Stream<ClassNode> parentValues = getNodesFromNames(parents);
		Stream<ClassNode> childrenValues = getNodesFromNames(children);
		// Get edges of parents/children
		Stream<Edge<ClassNode>> parentEdges = parentValues.map(node -> {
			ClassVertex other = graph.getRoot(node.name);
			if(other == null) {
				other = new ClassVertex(graph, node);
			}
			return new DirectedEdge<>(other, ClassVertex.this);
		});
		Stream<Edge<ClassNode>> childrenEdges = childrenValues.map(node -> {
			ClassVertex other = graph.getRoot(node.name);
			return new DirectedEdge<>(ClassVertex.this, other);
		});
		// Concat edges and return as set.
		return Stream.concat(parentEdges, childrenEdges)
				.collect(Collectors.toSet());
	}

	// ============================== UTILITY =================================== //

	/**
	 * @param names
	 * 		Stream of names of classes.
	 *
	 * @return Mapped stream where names are replaced with instances.
	 * If a name has no instance mapping, it is discarded.
	 */
	private Stream<ClassNode> getNodesFromNames(Stream<String> names) {
		return names.map(name -> {
			if(graph.getInput().classes.contains(name)) {
				return graph.getInput().getClass(name);
			}
			ClassNode node = loadNode(name);
			if(node != null) {
				return node;
			}
			return null;
		}).filter(node -> node != null);
	}

	/**
	 * @param name
	 * 		Internal class name.
	 *
	 * @return ClassNode loaded from runtime.
	 */
	private static ClassNode loadNode(String name) {
		try {
			Class<?> loaded = Classpath.getSystemClass(normalize(name));
			return ClassUtil.getNode(loaded);
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
