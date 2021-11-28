package me.coley.recaf.mapping;

import me.coley.recaf.mapping.impl.SimpleMappings;
import me.coley.recaf.mapping.impl.TinyV1Mappings;
import me.coley.recaf.plugin.tools.ToolManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
		register(new MappingsTool(TinyV1Mappings::new));
	}

	/**
	 * Update the aggregate mappings for the workspace.
	 *
	 * @param newMappings
	 * 		The additional mappings that were added.
	 * @param changedClasses
	 * 		The set of class names that have been updated as a result of the definition changes.
	 */
	public void updateAggregateMappings(Mappings newMappings, Set<String> changedClasses) {
		if (aggregatedMappings.update(newMappings, changedClasses)) {
			aggregateListeners.forEach(listener -> listener.onAggregatedMappingsUpdated(getAggregatedMappings()));
		}
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
