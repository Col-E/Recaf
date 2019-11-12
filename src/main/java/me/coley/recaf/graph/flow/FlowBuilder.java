package me.coley.recaf.graph.flow;

import me.coley.recaf.graph.*;
import org.objectweb.asm.*;

import java.util.*;

/**
 * Search implementation that builds an call graph.
 *
 * @author Matt
 */
public class FlowBuilder extends ClassDfsSearch implements ExhaustiveSearch<FlowVertex, ClassReader> {
	private final Map<String, Flow> vertices = new HashMap<>();
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
		Flow parent = null;
		if (path.size() > 0) {
			String last = path.get(path.size() -1).toString();
			parent = vertices.get(last);
		}
		String key = vertex.toString();
		Flow general = vertices.get(key);
		if(general == null) {
			general = new Flow(currentId++, (FlowVertex) vertex);
			vertices.put(key, general);
		}
		if(parent != null) {
			parent.children.add(general);
			general.parents.add(parent);
		}
	}

	@Override
	protected boolean shouldSkip(Vertex<ClassReader> vertex) {
		return super.shouldSkip(vertex);
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
	public Map<String, Flow> getVertices() {
		return vertices;
	}

	/**
	 * {@link FlowVertex} wrapper &amp; minimal static control flow vertex.
	 */
	public static class Flow implements Comparable<Flow> {
		private final List<Flow> parents = new ArrayList<>();
		private final List<Flow> children = new ArrayList<>();
		private final FlowVertex value;
		private final int id;

		/**
		 * Constructs a general vertex.
		 *
		 * @param id
		 * 		Vertex identifier.
		 * @param value
		 * 		Vertex value.
		 */
		public Flow(int id, FlowVertex value) {
			this.value = value;
			this.id = id;
		}

		/**
		 * @param other
		 * 		Another flow vertex belonging to a separate resource.
		 *
		 * @return The set of vertices attached to the base flow that do not have
		 * mappings to the vertices connected to the target flow. An empty set indicates
		 * the flow vertices model the same structure / call-graph.
		 */
		public Set<Flow> getDifference(Flow other) {
			return getDifference(new LinkedHashSet<>(), this, other);
		}

		private static Set<Flow> getDifference(Set<Flow> set, Flow vert1, Flow vert2) {
			if(vert1.children.size() != vert2.children.size()) {
				set.add(vert1);
				return set;
			} else {
				for(int i = 0; i < vert1.children.size() && set.isEmpty(); i++)
					getDifference(set, vert1.children.get(i), vert2.children.get(i));
				return set;
			}
		}

		/**
		 * Sorts children of the current flow vertex.
		 */
		public void sort() {
			Collections.sort(children);
		}

		/**
		 * <b>Ensure data is {@link #sort() sorted} before comparing!</b>
		 *
		 * @param other
		 * 		Another flow vertex to compare against.
		 *
		 * @return Comparison ordering favoring vertices containing larger children sets.
		 */
		@Override
		public int compareTo(Flow other) {
			if(children.size() != other.children.size()) {
				// sort larger items first
				return Integer.compare(children.size(), other.children.size());
			} else {
				// compare the children if this layer is equal
				for(int i = 0; i < children.size(); i++) {
					int result = children.get(i).compareTo(other.children.get(i));
					if(result != 0)
						return result;
				}
				return 0;
			}
		}

		/**
		 * @return Child vertices with edges connecting to the current flow vertex.
		 */
		public List<Flow> getChildren() {
			return children;
		}

		/**
		 * @return Parent vertices with edges connecting to the current flow vertex.
		 */
		public List<Flow> getParents() {
			return parents;
		}

		@Override
		public String toString() {
			return id + ":" + value;
		}

		/**
		 * @return Wrapped generative graph node.
		 */
		public FlowVertex getValue() {
			return value;
		}
	}
}