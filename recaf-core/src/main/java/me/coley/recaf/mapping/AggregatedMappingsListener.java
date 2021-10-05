package me.coley.recaf.mapping;

import java.util.Map;

/**
 * Listener for when the {@link MappingsManager#getAggregatedMappings() aggregated mappings} are updated.
 *
 * @author Matt Coley
 */
public interface AggregatedMappingsListener {
	/**
	 * Any update to the aggregated mappings will call this.
	 *
	 * @param mappings
	 * 		Current aggregated mappings.
	 */
	void onAggregatedMappingsUpdated(Map<String, String> mappings);
}
