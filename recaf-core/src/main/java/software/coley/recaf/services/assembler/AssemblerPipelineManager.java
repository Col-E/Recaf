package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.darknet.assembler.compiler.ClassRepresentation;
import me.darknet.assembler.compiler.ClassResult;
import software.coley.recaf.cdi.EagerInitialization;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.workspace.WorkspaceCloseListener;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.services.workspace.WorkspaceOpenListener;
import software.coley.recaf.workspace.model.EmptyWorkspace;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Objects;

/**
 * Assembler implementations manager.
 *
 * @author Justus Garbe
 */
@EagerInitialization
@ApplicationScoped
public class AssemblerPipelineManager implements Service {
	public static final String SERVICE_ID = "assembler-pipeline";
	private final WorkspaceManager workspaceManager;
	private final InheritanceGraphService graphService;
	private final JvmAssemblerPipelineConfig jvmConfig;
	private final AndroidAssemblerPipelineConfig androidConfig;
	private final AssemblerPipelineGeneralConfig config;
	private JvmAssemblerPipeline currentJvmPipeline;

	@Inject
	public AssemblerPipelineManager(@Nonnull WorkspaceManager workspaceManager,
	                                @Nonnull InheritanceGraphService graphService,
	                                @Nonnull AssemblerPipelineGeneralConfig config,
	                                @Nonnull AndroidAssemblerPipelineConfig androidConfig,
	                                @Nonnull JvmAssemblerPipelineConfig jvmConfig) {
		this.workspaceManager = workspaceManager;
		this.graphService = graphService;
		this.config = config;
		this.jvmConfig = jvmConfig;
		this.androidConfig = androidConfig;

		ListenerHost host = new ListenerHost();
		workspaceManager.addWorkspaceOpenListener(host);
		workspaceManager.addWorkspaceCloseListener(host);
	}

	/**
	 * Automatically pick a pipeline for the content in the given path.
	 *
	 * @param path
	 * 		Path to some item in the workspace to get an assembler pipeline for.
	 *
	 * @return Either a {@link JvmAssemblerPipeline} or {@link AndroidAssemblerPipeline} based on the path contents.
	 */
	@Nonnull
	public AssemblerPipeline<? extends ClassInfo, ? extends ClassResult, ? extends ClassRepresentation> getPipeline(@Nonnull PathNode<?> path) {
		ClassInfo info = path.getValueOfType(ClassInfo.class);
		if (info == null)
			throw new IllegalStateException("Failed to find class info for node: " + path);
		if (info.isJvmClass()) {
			Workspace workspace = Objects.requireNonNullElseGet(path.getValueOfType(Workspace.class), EmptyWorkspace::get);
			return newJvmAssemblerPipeline(workspace);
		} else {
			// TODO: Implement when dalvik assembler pipeline is implemented
			throw new UnsupportedOperationException("Dalvik assembler pipeline is not implemented");
		}
	}

	/**
	 * @param workspace
	 * 		Workspace to pull class data from.
	 *
	 * @return Assembler pipeline for JVM classes.
	 */
	@Nonnull
	public JvmAssemblerPipeline newJvmAssemblerPipeline(@Nonnull Workspace workspace) {
		if (currentJvmPipeline != null && workspace == workspaceManager.getCurrent())
			return currentJvmPipeline;

		InheritanceGraph graph = graphService.getOrCreateInheritanceGraph(workspace);
		return new JvmAssemblerPipeline(workspace, Objects.requireNonNull(graph), config, jvmConfig);
	}

	/**
	 * @param workspace
	 * 		Workspace to pull class data from.
	 *
	 * @return Assembler pipeline for Dalvik classes.
	 */
	@Nonnull
	public AndroidAssemblerPipeline newAndroidAssemblerPipeline(@Nonnull Workspace workspace) {
		return new AndroidAssemblerPipeline(config, androidConfig);
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public AssemblerPipelineGeneralConfig getServiceConfig() {
		return config;
	}

	private class ListenerHost implements WorkspaceOpenListener, WorkspaceCloseListener {
		@Override
		public void onWorkspaceOpened(@Nonnull Workspace workspace) {
			currentJvmPipeline = newJvmAssemblerPipeline(workspace);
		}

		@Override
		public void onWorkspaceClosed(@Nonnull Workspace workspace) {
			if (currentJvmPipeline != null) {
				currentJvmPipeline.close();
				currentJvmPipeline = null;
			}
		}
	}
}
