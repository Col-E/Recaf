package me.coley.recaf.graph;

import me.coley.recaf.util.ClassUtil;
import org.objectweb.asm.ClassReader;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Graph vertex with a {@link org.objectweb.asm.ClassReader} as the data.
 *
 * @param <G>
 * 		The type of graph holding the vertex.
 *
 * @author Matt
 */
public abstract class ClassVertex<G extends WorkspaceGraph> extends Vertex<ClassReader> {
	protected final G graph;
	private ClassReader clazz;

	/**
	 * Constructs a class vertex from the containing graph and class reader.
	 *
	 * @param graph
	 * 		The containing graph.
	 * @param clazz
	 * 		The vertex data.
	 */
	public ClassVertex(G graph, ClassReader clazz) {
		this.clazz = clazz;
		this.graph = graph;
	}

	/**
	 * @return Name of class stored by the vertex.
	 */
	public String getClassName() {
		return clazz.getClassName();
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
			throw new IllegalStateException("ClassVertex should not be compared to null");
		if(this == other)
			return true;
		if(other instanceof ClassVertex) {
			ClassVertex otherVertex = (ClassVertex) other;
			return getData().getClassName().equals(otherVertex.getData().getClassName());
		}
		return false;
	}

	@Override
	public String toString() {
		return getClassName();
	}

	// ============================== UTILITY =================================== //

	/**
	 * @param names
	 * 		Stream of names of classes.
	 *
	 * @return Mapped stream where names are replaced with instances.
	 * If a name has no instance mapping, it is discarded.
	 */
	protected Stream<ClassReader> getReadersFromNames(Stream<String> names) {
		return names.map(name -> {
			// Try loading from workspace
			ClassReader reader = graph.getWorkspace().getClassReader(name);
			if(reader != null)
				return reader;
			// Try loading from runtime
			return ClassUtil.fromRuntime(name);
		}).filter(Objects::nonNull);
	}
}
