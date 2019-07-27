package me.coley.recaf.mapping;

import me.coley.recaf.graph.flow.FlowBuilder;

import java.util.Set;

/**
 * Correlation analysis result for some entry point. The {@link #getBase() base} represents the
 * entry point in the reference program. The {@link #getTarget() target} represents the entry point in
 * another program, assumed to be the same but with renamed identifiers.
 *
 * @author Matt
 */
public class CorrelationResult {
	private final FlowBuilder.Flow base;
	private final FlowBuilder.Flow target;

	/**
	 * Constructs a correlation result.
	 *
	 * @param base
	 * 		Simplified flow graph of an entry point in the target resource.
	 * @param target
	 * 		Simplified flow graph of an entry point in the target resource.
	 */
	public CorrelationResult(FlowBuilder.Flow base, FlowBuilder.Flow target) {
		this.base = base;
		this.target = target;
	}

	/**
	 * @return Simplified flow graph of an entry point in the target resource.
	 */
	public FlowBuilder.Flow getBase() {
		return base;
	}

	/**
	 * @return Simplified flow graph of an entry point in the target resource.
	 */
	public FlowBuilder.Flow getTarget() {
		return target;
	}

	/**
	 * @return The set of vertices attached to the base flow that do not have
	 * mappings to the vertices connected to the target flow. An empty set indicates
	 * the flow vertices model the same structure / call-graph.
	 */
	public Set<FlowBuilder.Flow> getDifference() {
		return base.getDifference(target);
	}
}