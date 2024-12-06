package software.coley.recaf.services.callgraph;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.cdi.EagerInitialization;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.workspace.WorkspaceCloseListener;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.services.workspace.WorkspaceOpenListener;
import software.coley.recaf.workspace.model.Workspace;

import java.util.concurrent.CompletableFuture;

/**
 * Service offering the creation of {@link CallGraph call graphs} for workspaces.
 *
 * @author Matt Coley
 * @see CallGraph
 */
@EagerInitialization
@ApplicationScoped
public class CallGraphService implements Service {
	public static final String SERVICE_ID = "graph-calls";
	private static final DebuggingLogger logger = Logging.get(CallGraphService.class);
	private final CallGraphConfig config;
	private CallGraph currentWorkspaceGraph;

	/**
	 * @param workspaceManager
	 * 		Manager to register listeners for, in order to manage a shared graph for the current workspace.
	 * @param config
	 * 		Graphing config options.
	 */
	@Inject
	public CallGraphService(@Nonnull WorkspaceManager workspaceManager, @Nonnull CallGraphConfig config) {
		this.config = config;

		ListenerHost host = new ListenerHost();
		workspaceManager.addWorkspaceOpenListener(host);
		workspaceManager.addWorkspaceCloseListener(host);
	}

	/**
	 * Creates a new call graph for the given workspace.
	 * Before you use the graph, you will need to call {@link CallGraph#initialize()}.
	 *
	 * @param workspace
	 * 		Workspace to pull classes from.
	 *
	 * @return New call graph model for the given workspace.
	 */
	@Nonnull
	public CallGraph newCallGraph(@Nonnull Workspace workspace) {
		return new CallGraph(workspace);
	}

	/**
	 * @return Call graph model for the {@link WorkspaceManager#getCurrent() current workspace}
	 * or {@code null} if no workspace is currently open.
	 */
	@Nullable
	public CallGraph getCurrentWorkspaceCallGraph() {
		CallGraph graph = currentWorkspaceGraph;

		// Lazily initialize the graph so that we don't do a full graph
		if (!graph.isInitialized())
			CompletableFuture.runAsync(graph::initialize);

		return graph;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public CallGraphConfig getServiceConfig() {
		return config;
	}

	private class ListenerHost implements WorkspaceOpenListener, WorkspaceCloseListener {
		@Override
		public void onWorkspaceOpened(@Nonnull Workspace workspace) {
			currentWorkspaceGraph = newCallGraph(workspace);
		}

		@Override
		public void onWorkspaceClosed(@Nonnull Workspace workspace) {
			currentWorkspaceGraph = null;
		}
	}
}
