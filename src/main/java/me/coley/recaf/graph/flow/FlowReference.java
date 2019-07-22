package me.coley.recaf.graph.flow;

import java.util.Objects;

/**
 * Method call outline.
 *
 * @author Matt
 */
public class FlowReference {
	private final FlowVertex vertex;
	private final String name;
	private final String desc;

	/**
	 * Constructs a method call reference for flow graphing.
	 *
	 * @param vertex
	 * 		Vertex of the class containing the called method.
	 * @param name
	 * 		Name of the method called.
	 * @param desc
	 * 		Descriptor of the method called.
	 */
	public FlowReference(FlowVertex vertex, String name, String desc) {
		this.vertex = vertex;
		this.name = name;
		this.desc = desc;
	}

	/**
	 * @return Vertex of the class containing the called method.
	 */
	public FlowVertex getVertex() {
		return vertex;
	}

	/**
	 * @return Name of the method called.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Descriptor of the method called.
	 */
	public String getDesc() {
		return desc;
	}

	@Override
	public String toString() {
		return vertex.getData().getClassName() + "." + name + desc;
	}

	@Override
	public int hashCode() {
		return Objects.hash(vertex.getData().getClassName(), name, desc);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other instanceof FlowReference) {
			FlowReference frOther = (FlowReference) other;
			return vertex.equals(frOther.vertex) &&
						name.equals(frOther.name) &&
						desc.equals(frOther.desc);
		}
		return false;
	}
}