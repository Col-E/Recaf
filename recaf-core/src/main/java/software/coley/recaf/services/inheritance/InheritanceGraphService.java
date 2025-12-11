package software.coley.recaf.services.inheritance;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.cdi.EagerInitialization;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.mapping.MappingListeners;
import software.coley.recaf.services.workspace.WorkspaceCloseListener;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Objects;

/**
 * Service offering the creation of {@link InheritanceGraph inheritance graphs} for workspaces.
 *
 * @author Matt Coley
 * @see InheritanceGraph
 */
@EagerInitialization
@ApplicationScoped
public class InheritanceGraphService implements Service {
	public static final String SERVICE_ID = "graph-inheritance";
	private final InheritanceGraphServiceConfig config;
	private final MappingListeners mappingListeners;
	private final WorkspaceManager workspaceManager;
	private volatile InheritanceGraph currentWorkspaceGraph;

	@Inject
	public InheritanceGraphService(@Nonnull WorkspaceManager workspaceManager,
	                               @Nonnull MappingListeners mappingListeners,
	                               @Nonnull InheritanceGraphServiceConfig config) {
		this.workspaceManager = workspaceManager;
		this.mappingListeners = mappingListeners;
		this.config = config;

		ListenerHost host = new ListenerHost();
		workspaceManager.addWorkspaceCloseListener(host);
	}

	/**
	 * Gets an existing graph if present for the workspace,
	 * or makes a new one if there is no associated graph for the workspace.
	 *
	 * @param workspace
	 * 		Workspace to pull classes from.
	 *
	 * @return Inheritance graph model for the given workspace.
	 */
	@Nonnull
	public InheritanceGraph getOrCreateInheritanceGraph(@Nonnull Workspace workspace) {
		return workspaceManager.getCurrent() == workspace ?
				Objects.requireNonNull(getCurrentWorkspaceInheritanceGraph(), "Failed to get current workspace graph") :
				newInheritanceGraph(workspace);
	}

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 *
	 * @return New inheritance graph model for the given workspace.
	 */
	@Nonnull
	public InheritanceGraph newInheritanceGraph(@Nonnull Workspace workspace) {
		return new InheritanceGraph(workspace);
	}

	/**
	 * @return Inheritance graph model for the {@link WorkspaceManager#getCurrent() current workspace}
	 * or {@code null} if no workspace is currently open.
	 */
	@Nullable
	public InheritanceGraph getCurrentWorkspaceInheritanceGraph() {
		if (!workspaceManager.hasCurrentWorkspace())
			return null;

		// Building graphs can be expensive for large workspaces, so to prevent races we will double-check.
		if (currentWorkspaceGraph == null) {
			synchronized (this) {
				if (currentWorkspaceGraph == null) {
					InheritanceGraph graph = newInheritanceGraph(workspaceManager.getCurrent());
					mappingListeners.addMappingApplicationListener(graph);
					currentWorkspaceGraph = graph;
				}
			}
		}

		return currentWorkspaceGraph;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public InheritanceGraphServiceConfig getServiceConfig() {
		return config;
	}

	private class ListenerHost implements WorkspaceCloseListener {
		@Override
		public void onWorkspaceClosed(@Nonnull Workspace workspace) {
			if (currentWorkspaceGraph != null) {
				// Remove the graph as a listener so that it can be feed by the garbage collector.
				mappingListeners.removeMappingApplicationListener(currentWorkspaceGraph);

				// Notify the graph of closure, then purge the reference.
				currentWorkspaceGraph.onWorkspaceClosed(workspace);
				currentWorkspaceGraph = null;
			}
		}
	}
}