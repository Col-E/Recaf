package software.coley.recaf.services.mapping.aggregate;

/**
 * Listener for when the {@link AggregatedMappings aggregated mappings} are updated.
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
