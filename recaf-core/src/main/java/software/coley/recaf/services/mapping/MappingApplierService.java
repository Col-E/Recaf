package software.coley.recaf.services.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.mapping.aggregate.AggregateMappingManager;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Service offering the creation of {@link MappingApplier mapping appliers} for workspaces.
 *
 * @author Matt Coley
 * @see MappingApplier
 */
@ApplicationScoped
public class MappingApplierService implements Service {
	public static final String SERVICE_ID = "mapping-applier";
	private static final ExecutorService applierThreadPool = ThreadPoolFactory.newFixedThreadPool(SERVICE_ID);
	private final InheritanceGraphService inheritanceGraphService;
	private final AggregateMappingManager aggregateMappingManager;
	private final MappingListeners listeners;
	private final WorkspaceManager workspaceManager;
	private final MappingApplierConfig config;

	@Inject
	public MappingApplierService(@Nonnull MappingApplierConfig config,
	                             @Nonnull InheritanceGraphService inheritanceGraphService,
	                             @Nonnull AggregateMappingManager aggregateMappingManager,
	                             @Nonnull MappingListeners listeners,
	                             @Nonnull WorkspaceManager workspaceManager) {
		this.inheritanceGraphService = inheritanceGraphService;
		this.aggregateMappingManager = aggregateMappingManager;
		this.listeners = listeners;
		this.workspaceManager = workspaceManager;
		this.config = config;
	}

	/**
	 * @param workspace
	 * 		Workspace to apply mappings in.
	 *
	 * @return Applier for the given workspace.
	 */
	@Nonnull
	public MappingApplier inWorkspace(@Nonnull Workspace workspace) {
		if (workspace == workspaceManager.getCurrent())
			return Objects.requireNonNull(inCurrentWorkspace(), "Failed to access current workspace for mapping application");
		return new MappingApplier(workspace, inheritanceGraphService.newInheritanceGraph(workspace), null, null);
	}

	/**
	 * @return Applier for the current workspace, or {@code null} if no workspace is open.
	 */
	@Nullable
	public MappingApplier inCurrentWorkspace() {
		Workspace workspace = workspaceManager.getCurrent();
		if (workspace == null)
			return null;
		InheritanceGraph currentWorkspaceInheritanceGraph = inheritanceGraphService.getCurrentWorkspaceInheritanceGraph();
		if (currentWorkspaceInheritanceGraph == null)
			return null;
		return new MappingApplier(workspace, currentWorkspaceInheritanceGraph, listeners, aggregateMappingManager);
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public MappingApplierConfig getServiceConfig() {
		return config;
	}
}
