package software.coley.recaf.services.mapping.aggregate;

import jakarta.annotation.Nonnull;
import software.coley.recaf.behavior.PrioritySortable;

/**
 * Listener for when the {@link AggregatedMappings aggregated mappings} are updated.
 *
 * @author Matt Coley
 */
public interface AggregatedMappingsListener extends PrioritySortable {
	/**
	 * Any update to the aggregated mappings will call this.
	 *
	 * @param mappings
	 * 		Current aggregated mappings.
	 */
	void onAggregatedMappingsUpdated(@Nonnull AggregatedMappings mappings);
}
