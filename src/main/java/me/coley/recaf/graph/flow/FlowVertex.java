package me.coley.recaf.graph.flow;

import me.coley.recaf.graph.*;
import org.objectweb.asm.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.objectweb.asm.ClassReader.*;

/**
 * ClassVertex implementation with outbound method calls as the edge source.
 *
 * @author Matt
 */
public class FlowVertex extends ClassVertex<FlowGraph> {
	private final String name;
	private final String desc;

	/**
	 * Constructs a flow vertex from the containing graph and class reader.<br>
	 * The vertex contains method data, thus outlines a method.
	 *
	 * @param graph
	 * 		The containing graph.
	 * @param clazz
	 * 		The vertex data.
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 */
	public FlowVertex(FlowGraph graph, ClassReader clazz, String name, String desc) {
		super(graph, clazz);
		this.name = name;
		this.desc = desc;
	}

	@Override
	public Set<Edge<ClassReader>> getEdges() {
		// Due to the generative nature of the graphing api, we can only track outbound calls from
		// the context of a single vertex.
		return getOutbound().stream()
				.map(FlowReference::getVertex)
				.map(vert -> new DirectedEdge<>(this, vert))
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private Set<FlowReference> getOutbound() {
		// Only search if the method has been specified
		if (name == null || desc == null)
			return Collections.emptySet();
		// Collect & return references.
		OutboundCollector collector = new OutboundCollector(graph, name, desc);
		getData().accept(collector, SKIP_DEBUG | SKIP_FRAMES);
		return collector.getOutbound();
	}

	/**
	 * @return Name of the class containing the method represented.
	 */
	public String getOwner() {
		return getData().getClassName();
	}

	/**
	 * @return Method name of the vertex.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Method descriptor of the vertex.
	 */
	public String getDesc() {
		return desc;
	}

	@Override
	public String toString() {
		return getData().getClassName() + "." + name + desc;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getData().getClassName(), name, desc);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other instanceof FlowVertex) {
			FlowVertex fvOther = (FlowVertex) other;
			String owner = getData().getClassName();
			String frOwner = fvOther.getData().getClassName();
			return owner.equals(frOwner) &&
					name.equals(fvOther.name) &&
					desc.equals(fvOther.desc);
		}
		return false;
	}
}
