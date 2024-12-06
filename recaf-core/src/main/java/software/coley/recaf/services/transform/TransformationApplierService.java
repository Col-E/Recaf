package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Objects;

/**
 * Service offering the creation of {@link TransformationApplier transformation appliers} for workspaces.
 *
 * @author Matt Coley
 * @see TransformationManager
 * @see TransformationApplier
 */
@ApplicationScoped
public class TransformationApplierService implements Service {
	public static final String SERVICE_ID = "transformation-applier";
	private static final Logger logger = Logging.get(TransformationApplierService.class);
	private final TransformationManager transformationManager;
	private final InheritanceGraphService graphService;
	private final WorkspaceManager workspaceManager;
	private final TransformationApplierConfig config;

	@Inject
	public TransformationApplierService(@Nonnull TransformationManager transformationManager,
	                                    @Nonnull InheritanceGraphService graphService,
	                                    @Nonnull WorkspaceManager workspaceManager,
	                                    @Nonnull TransformationApplierConfig config) {
		this.graphService = graphService;
		this.workspaceManager = workspaceManager;
		this.transformationManager = transformationManager;
		this.config = config;
	}

	/**
	 * @param workspace
	 * 		Workspace to apply transformations within.
	 *
	 * @return Transformation applier for the given workspace.
	 */
	@Nonnull
	public TransformationApplier newApplier(@Nonnull Workspace workspace) {
		// Optimal case for current workspace using the shared workspace inheritance graph
		if (workspace == workspaceManager.getCurrent())
			return newApplier(workspace, Objects.requireNonNull(graphService.getCurrentWorkspaceInheritanceGraph(), "Graph not created"));

		// Need to make a new graph for the given workspace
		InheritanceGraph inheritanceGraph = graphService.newInheritanceGraph(workspace);
		return newApplier(workspace, inheritanceGraph);
	}

	/**
	 * @return Transformation applier for the {@link WorkspaceManager#getCurrent() current workspace}
	 * or {@code null} if no workspace is currently open.
	 */
	@Nullable
	public TransformationApplier newApplierForCurrentWorkspace() {
		Workspace workspace = workspaceManager.getCurrent();
		if (workspace == null)
			return null;
		InheritanceGraph inheritanceGraph = Objects.requireNonNull(graphService.getCurrentWorkspaceInheritanceGraph(), "Graph not created");
		return newApplier(workspace, inheritanceGraph);
	}

	/**
	 * @param workspace
	 * 		Workspace to apply transformations within.
	 * @param inheritanceGraph
	 * 		Inheritance graph for the given workspace.
	 *
	 * @return Transformation applier for the given workspace.
	 */
	@Nonnull
	private TransformationApplier newApplier(@Nonnull Workspace workspace, @Nonnull InheritanceGraph inheritanceGraph) {
		return new TransformationApplier(transformationManager, inheritanceGraph, workspace);
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public TransformationApplierConfig getServiceConfig() {
		return config;
	}
}
