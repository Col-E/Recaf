package me.coley.recaf.graph.flow;

import me.coley.recaf.graph.*;
import org.objectweb.asm.ClassReader;

import java.util.*;

/**
 * Search implementation that builds an call graph.
 *
 * @author Matt
 */
public class FlowBuilder extends ClassDfsSearch implements ExhaustiveSearch<FlowVertex, ClassReader> {
	private final Map<String, GeneralVertex> vertices = new HashMap<>();
	private final Map<Integer, GeneralVertex> idLookup = new HashMap<>();
	private int currentId;

	/**
	 * Constructs a method call flow builder.
	 */
	public FlowBuilder() {
		super(Type.CHILDREN);
	}

	@Override
	public void onVisit(List<Vertex<ClassReader>> path, Vertex<ClassReader> vertex) {
		super.onVisit(path, vertex);
		GeneralVertex parent = null;
		if (path.size() > 0) {
			String last = path.get(path.size() -1).toString();
			parent = vertices.get(last);
		}
		String key = vertex.toString();
		GeneralVertex connection = null; // vertices.get(key);
		if(connection == null) {
			connection = new GeneralVertex(currentId++, (FlowVertex) vertex, parent);
			vertices.put(key, connection);
			idLookup.put(connection.id, connection);
		}
		if(parent != null)
			parent.children.add(connection);
	}

	@Override
	protected boolean shouldSkip(Vertex<ClassReader> vertex) {
		return false;
	}

	@Override
	public Vertex<ClassReader> dummy() {
		return new FlowVertex(null, new ClassReader(DUMMY_CLASS_BYTECODE), "<clinit>", "()V") {
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
				if(other instanceof FlowVertex)
					return hashCode() == other.hashCode();
				return false;
			}

			@Override
			public String toString() {
				return "[[Dummy]]";
			}
		};
	}

	/**
	 * @return Map of visited vertices. Keys are the visited
	 * {@link me.coley.recaf.graph.flow.FlowVertex} string representations.
	 */
	public Map<String, GeneralVertex> getVertices() {
		return vertices;
	}

	public static class GeneralVertex implements Comparable<GeneralVertex> {
		private final GeneralVertex parent;
		private final Set<GeneralVertex> children = new HashSet<>();
		private final FlowVertex value;
		private final int id;

		/**
		 * Constructs a general vertex.
		 *
		 * @param id
		 * 		Vertex identifier.
		 * @param value
		 * 		Vertex value.
		 * @param parent
		 * 		Parent vertex. May be {@code null}.
		 */
		public GeneralVertex(int id, FlowVertex value, GeneralVertex parent) {
			this.value = value;
			this.id = id;
			this.parent = parent;
		}

		@Override
		public String toString() {
			return  id + ":" + children.size() + ":" + value;
		}

		@Override
		public int hashCode() {
			if (parent != null)
				return Objects.hash(children.size(), parent.children.size());
			return children.size();
		}

		@Override
		public boolean equals(Object other) {
			if(other instanceof GeneralVertex) {
				GeneralVertex vother = (GeneralVertex) other;
				return children.size() == vother.children.size();
			}
			return false;
		}

		@Override
		public int compareTo(GeneralVertex o) {
			if (id == o.id) return 0;
			return id > o.id ? 1 : -1;
		}
	}
}
