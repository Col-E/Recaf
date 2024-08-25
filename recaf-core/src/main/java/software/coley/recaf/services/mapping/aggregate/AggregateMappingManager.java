package software.coley.recaf.services.mapping.aggregate;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.cdi.AutoRegisterWorkspaceListeners;
import software.coley.recaf.cdi.WorkspaceScoped;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.workspace.WorkspaceCloseListener;
import software.coley.recaf.workspace.model.Workspace;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages tracking the state of mappings over time.
 *
 * @author Matt Coley
 * @author Marius Renner
 */
@WorkspaceScoped
@AutoRegisterWorkspaceListeners
public class AggregateMappingManager implements Service, WorkspaceCloseListener {
	public static final String SERVICE_ID = "mapping-aggregator";
	private static final Logger logger = Logging.get(AggregateMappingManager.class);
	private final List<AggregatedMappingsListener> aggregateListeners = new CopyOnWriteArrayList<>();
	private final AggregatedMappings aggregatedMappings;
	private final AggregateMappingManagerConfig config;

	@Inject
	public AggregateMappingManager(@Nonnull AggregateMappingManagerConfig config,
	                               @Nonnull Workspace workspace) {
		this.config = config;
		aggregatedMappings = new AggregatedMappings(workspace);
	}

	@Override
	public void onWorkspaceClosed(@Nonnull Workspace workspace) {
		aggregateListeners.clear();
		clearAggregated();
	}

	/**
	 * Update the aggregate mappings for the workspace.
	 *
	 * @param newMappings
	 * 		The additional mappings that were added.
	 */
	public void updateAggregateMappings(@Nonnull Mappings newMappings) {
		aggregatedMappings.update(newMappings);
		Unchecked.checkedForEach(aggregateListeners, listener -> listener.onAggregatedMappingsUpdated(getAggregatedMappings()),
				(listener, t) -> logger.error("Exception thrown when updating aggregate mappings", t));
	}

	/**
	 * Clears all mapping information.
	 */
	private void clearAggregated() {
		aggregatedMappings.clear();
		Unchecked.checkedForEach(aggregateListeners, listener -> listener.onAggregatedMappingsUpdated(getAggregatedMappings()),
				(listener, t) -> logger.error("Exception thrown when updating aggregate mappings", t));
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addAggregatedMappingsListener(@Nonnull AggregatedMappingsListener listener) {
		aggregateListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 *
	 * @return {@code true} when the listener was removed.
	 * {@code false} if the listener was not in the list.
	 */
	public boolean removeAggregatedMappingListener(@Nonnull AggregatedMappingsListener listener) {
		return aggregateListeners.remove(listener);
	}

	/**
	 * @return Current aggregated mappings in the ASM format.
	 */
	@Nonnull
	public AggregatedMappings getAggregatedMappings() {
		return aggregatedMappings;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public AggregateMappingManagerConfig getServiceConfig() {
		return config;
	}
}
