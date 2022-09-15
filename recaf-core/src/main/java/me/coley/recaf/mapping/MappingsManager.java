package me.coley.recaf.mapping;

import me.coley.recaf.mapping.format.*;
import me.coley.recaf.plugin.tools.ToolManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages mapping tool implementations and tracks the state of mappings over time.
 *
 * @author Matt Coley
 * @author Marius Renner
 * @author Wolfie / win32kbase
 */
public class MappingsManager extends ToolManager<MappingsTool> {
	private final AggregatedMappings aggregatedMappings = new AggregatedMappings();
	private final List<AggregatedMappingsListener> aggregateListeners = new ArrayList<>();

	/**
	 * Registers all mapping tools.
	 */
	public MappingsManager() {
		register(new MappingsTool(SimpleMappings::new));
		register(new MappingsTool(EnigmaMappings::new));
		register(new MappingsTool(TinyV1Mappings::new));
		register(new MappingsTool(JadxMappings::new));
		register(new MappingsTool(SrgMappings::new));
		register(new MappingsTool(ProguardMappings::new));
	}

	/**
	 * Update the aggregate mappings for the workspace.
	 *
	 * @param newMappings
	 * 		The additional mappings that were added.
	 */
	public void updateAggregateMappings(Mappings newMappings) {
		aggregatedMappings.update(newMappings);
		aggregateListeners.forEach(listener -> listener.onAggregatedMappingsUpdated(getAggregatedMappings()));
	}

	/**
	 * Clears all mapping information.
	 */
	public void clearAggregated() {
		aggregatedMappings.clear();
		aggregateListeners.forEach(listener -> listener.onAggregatedMappingsUpdated(getAggregatedMappings()));
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addAggregatedMappingsListener(AggregatedMappingsListener listener) {
		aggregateListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 *
	 * @return {@code true} when the listener was removed.
	 * {@code false} if the listener was not in the list.
	 */
	public boolean removeAggregatedMappingListener(AggregatedMappingsListener listener) {
		return aggregateListeners.remove(listener);
	}

	/**
	 * @return Current aggregated mappings in the ASM format.
	 */
	public AggregatedMappings getAggregatedMappings() {
		return aggregatedMappings;
	}
}
