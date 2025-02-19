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
	private InheritanceGraph currentWorkspaceGraph;

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
		InheritanceGraph graph = new InheritanceGraph(config, workspace);
		workspace.addWorkspaceModificationListener(graph);
		return graph;
	}

	/**
	 * @return Inheritance graph model for the {@link WorkspaceManager#getCurrent() current workspace}
	 * or {@code null} if no workspace is currently open.
	 */
	@Nullable
	public InheritanceGraph getCurrentWorkspaceInheritanceGraph() {
		if (!workspaceManager.hasCurrentWorkspace())
			return null;

		if (currentWorkspaceGraph == null) {
			Workspace workspace = workspaceManager.getCurrent();
			InheritanceGraph graph = newInheritanceGraph(workspace);
			currentWorkspaceGraph = graph;

			// Add listener to handle updating the graph when renaming is applied to the current workspace.
			mappingListeners.addMappingApplicationListener(graph);
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
				currentWorkspaceGraph.onWorkspaceClosed(workspace);
				currentWorkspaceGraph = null;
			}
		}
	}
}