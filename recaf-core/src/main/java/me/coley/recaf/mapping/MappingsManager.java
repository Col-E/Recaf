package me.coley.recaf.mapping;

import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.util.*;

/**
 * Tracks the state of mappings over time.
 *
 * @author Matt Coley
 * @author Marius Renner
 */
public class MappingsManager {
	private static final Logger logger = Logging.get(MappingsManager.class);
	private final Map<String, String> aggregatedMappings = new TreeMap<>();
	private final List<AggregatedMappingsListener> listeners = new ArrayList<>();

	/**
	 * Update the aggregate ASM mappings in the workspace.
	 *
	 * @param newMappings
	 * 		The additional ASM mappings that were added.
	 * @param changedClasses
	 * 		The set of class names that have been updated as a result of the definition changes.
	 */
	public void updateAggregateMappings(Map<String, String> newMappings, Set<String> changedClasses) {
		Map<String, String> usefulMappings = new HashMap<>();
		for (Map.Entry<String, String> newMapping : newMappings.entrySet()) {
			// only process mappings that actually caused changes in their own class
			String className = AggregatedMappingUtils.getClassNameFromKey(newMapping.getKey());
			if (!changedClasses.contains(className)) {
				logger.trace("Omitting unused mapping: " + newMapping.getKey() + " -> " + newMapping.getValue());
				continue;
			}
			usefulMappings.put(newMapping.getKey(), newMapping.getValue());
		}
		if (!usefulMappings.isEmpty()) {
			listeners.forEach(listener -> listener.onAggregatedMappingsUpdated(getAggregatedMappings()));
			AggregatedMappingUtils.applyMappingToExisting(aggregatedMappings, usefulMappings);
		}
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addListener(AggregatedMappingsListener listener) {
		listeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 *
	 * @return {@code true} when the listener was removed.
	 * {@code false} if the listener was not in the list.
	 */
	public boolean removeListener(AggregatedMappingsListener listener) {
		return listeners.remove(listener);
	}

	/**
	 * Clears all mapping information.
	 */
	public void reset() {
		if (aggregatedMappings.size() > 0) {
			aggregatedMappings.clear();
			listeners.forEach(listener -> listener.onAggregatedMappingsUpdated(getAggregatedMappings()));
		}
	}

	/**
	 * @return Current aggregated mappings in the ASM format.
	 */
	public Map<String, String> getAggregatedMappings() {
		return Collections.unmodifiableMap(aggregatedMappings);
	}
}
