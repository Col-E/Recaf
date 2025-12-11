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
import software.coley.recaf.services.mapping.MappingApplier;
import software.coley.recaf.services.mapping.MappingApplierService;
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
	private final MappingApplierService mappingService;
	private final WorkspaceManager workspaceManager;
	private final TransformationApplierConfig config;
	private final TransformationApplierConfig applierConfig;

	@Inject
	public TransformationApplierService(@Nonnull TransformationManager transformationManager,
	                                    @Nonnull InheritanceGraphService graphService,
	                                    @Nonnull MappingApplierService mappingService,
	                                    @Nonnull WorkspaceManager workspaceManager,
	                                    @Nonnull TransformationApplierConfig config,
	                                    @Nonnull TransformationApplierConfig applierConfig) {
		this.graphService = graphService;
		this.mappingService = mappingService;
		this.workspaceManager = workspaceManager;
		this.transformationManager = transformationManager;
		this.config = config;
		this.applierConfig = applierConfig;
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
		if (workspace == workspaceManager.getCurrent()) {
			InheritanceGraph graphNotCreated = Objects.requireNonNull(graphService.getCurrentWorkspaceInheritanceGraph(), "Graph not created");
			MappingApplier mappingApplier = Objects.requireNonNull(mappingService.inCurrentWorkspace(), "Mapping applier not created");
			return newApplier(workspace, graphNotCreated, mappingApplier);
		}

		// Need to make a new graph for the given workspace
		InheritanceGraph inheritanceGraph = graphService.newInheritanceGraph(workspace);
		MappingApplier mappingApplier = mappingService.inWorkspace(workspace);
		return newApplier(workspace, inheritanceGraph, mappingApplier);
	}

	/**
	 * @return Transformation applier for the {@link WorkspaceManager#getCurrent() current workspace}
	 * or {@code null} if no workspace is currently open.
	 */
	@Nullable
	public TransformationApplier newApplierForCurrentWorkspace() {
		if (!workspaceManager.hasCurrentWorkspace())
			return null;
		InheritanceGraph inheritanceGraph = Objects.requireNonNull(graphService.getCurrentWorkspaceInheritanceGraph(), "Graph not created");
		MappingApplier mappingApplier = Objects.requireNonNull(mappingService.inCurrentWorkspace(), "Mapping applier not created");
		Workspace workspace = workspaceManager.getCurrent();
		return newApplier(workspace, inheritanceGraph, mappingApplier);
	}

	/**
	 * @param workspace
	 * 		Workspace to apply transformations within.
	 * @param inheritanceGraph
	 * 		Inheritance graph for the given workspace.
	 * @param mappingApplier
	 * 		Mapping applier for the given workspace.
	 *
	 * @return Transformation applier for the given workspace.
	 */
	@Nonnull
	private TransformationApplier newApplier(@Nonnull Workspace workspace, @Nonnull InheritanceGraph inheritanceGraph,
	                                         @Nonnull MappingApplier mappingApplier) {
		return new TransformationApplier(transformationManager, applierConfig, inheritanceGraph, mappingApplier, workspace);
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
