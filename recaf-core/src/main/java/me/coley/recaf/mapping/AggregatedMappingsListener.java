package me.coley.recaf.mapping;

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
	void onAggregatedMappingsUpdated(AggregatedMappings mappings);
}
