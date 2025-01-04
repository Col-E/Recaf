package software.coley.recaf.services.mapping.aggregate;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.workspace.WorkspaceCloseListener;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.services.workspace.WorkspaceOpenListener;
import software.coley.recaf.workspace.model.Workspace;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages tracking the state of mappings over time.
 *
 * @author Matt Coley
 * @author Marius Renner
 */
@ApplicationScoped
public class AggregateMappingManager implements Service, WorkspaceCloseListener {
	public static final String SERVICE_ID = "mapping-aggregator";
	private static final Logger logger = Logging.get(AggregateMappingManager.class);
	private final List<AggregatedMappingsListener> aggregateListeners = new CopyOnWriteArrayList<>();
	private final AggregateMappingManagerConfig config;
	private AggregatedMappings aggregatedMappings;

	@Inject
	public AggregateMappingManager(@Nonnull AggregateMappingManagerConfig config,
	                               @Nonnull WorkspaceManager workspaceManager) {
		this.config = config;

		ListenerHost host = new ListenerHost();
		workspaceManager.addWorkspaceOpenListener(host);
		workspaceManager.addWorkspaceCloseListener(host);
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
		if (aggregatedMappings == null)
			return;
		aggregatedMappings.update(newMappings);
		Unchecked.checkedForEach(aggregateListeners, listener -> listener.onAggregatedMappingsUpdated(getAggregatedMappings()),
				(listener, t) -> logger.error("Exception thrown when updating aggregate mappings", t));
	}

	/**
	 * Clears all mapping information.
	 */
	private void clearAggregated() {
		if (aggregatedMappings == null)
			return;
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
	 * @return Current aggregated mappings in the ASM format. Will be {@code null} if no workspace is open.
	 */
	@Nullable
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

	private class ListenerHost implements WorkspaceOpenListener, WorkspaceCloseListener {
		@Override
		public void onWorkspaceOpened(@Nonnull Workspace workspace) {
			aggregatedMappings = new AggregatedMappings(workspace);
		}

		@Override
		public void onWorkspaceClosed(@Nonnull Workspace workspace) {
			aggregatedMappings = null;
		}
	}
}
