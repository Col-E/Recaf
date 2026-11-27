package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
	private final InheritanceGraphService graphService;
	private final JvmAssemblerPipelineConfig jvmConfig;
	private final AndroidAssemblerPipelineConfig androidConfig;
	private final AssemblerPipelineGeneralConfig config;
	private JvmAssemblerPipeline currentJvmPipeline;
	private AndroidAssemblerPipeline currentAndroidPipeline;
	private Workspace currentWorkspace;

	@Inject
	public AssemblerPipelineManager(@Nonnull WorkspaceManager workspaceManager,
	                                @Nonnull InheritanceGraphService graphService,
	                                @Nonnull AssemblerPipelineGeneralConfig config,
	                                @Nonnull AndroidAssemblerPipelineConfig androidConfig,
	                                @Nonnull JvmAssemblerPipelineConfig jvmConfig) {
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
	public AssemblerPipeline<? extends ClassInfo, ? extends ClassResult<?>, ?> getPipeline(@Nonnull PathNode<?> path) {
		ClassInfo info = path.getValueOfType(ClassInfo.class);
		if (info == null)
			throw new IllegalStateException("Failed to find class info for node: " + path);
		if (info.isJvmClass()) {
			Workspace workspace = Objects.requireNonNullElseGet(path.getValueOfType(Workspace.class), EmptyWorkspace::get);
			return newJvmAssemblerPipeline(workspace);
		} else if (info.isAndroidClass()) {
			Workspace workspace = Objects.requireNonNullElseGet(path.getValueOfType(Workspace.class), EmptyWorkspace::get);
			return newAndroidAssemblerPipeline(workspace);
		} else {
			throw new UnsupportedOperationException("Unknown class type: " + info.getClass().getSimpleName());
		}
	}

	/**
	 * @param workspace
	 * 		Workspace to pull class data from.
	 *
	 * @return Assembler pipeline for JVM classes.
	 */
	@Nonnull
	public synchronized JvmAssemblerPipeline newJvmAssemblerPipeline(@Nonnull Workspace workspace) {
		boolean isOpen = workspace == currentWorkspace;
		if (isOpen && currentJvmPipeline != null)
			return currentJvmPipeline;

		InheritanceGraph graph = graphService.getOrCreateInheritanceGraph(workspace);
		JvmAssemblerPipeline pipeline = new JvmAssemblerPipeline(workspace, Objects.requireNonNull(graph), config, jvmConfig);
		if (isOpen)
			currentJvmPipeline = pipeline;
		return pipeline;
	}

	/**
	 * @param workspace
	 * 		Workspace to pull class data from.
	 *
	 * @return Assembler pipeline for Dalvik classes.
	 */
	@Nonnull
	public synchronized AndroidAssemblerPipeline newAndroidAssemblerPipeline(@Nonnull Workspace workspace) {
		boolean isOpen = workspace == currentWorkspace;
		if (isOpen && currentAndroidPipeline != null)
			return currentAndroidPipeline;

		InheritanceGraph graph = graphService.getOrCreateInheritanceGraph(workspace);
		AndroidAssemblerPipeline pipeline = new AndroidAssemblerPipeline(Objects.requireNonNull(graph), config, androidConfig);
		if (isOpen)
			currentAndroidPipeline = pipeline;
		return pipeline;
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
			synchronized (AssemblerPipelineManager.this) {
				currentWorkspace = workspace;
			}
		}

		@Override
		public void onWorkspaceClosed(@Nonnull Workspace workspace) {
			synchronized (AssemblerPipelineManager.this) {
				currentWorkspace = null;

				if (currentJvmPipeline != null) {
					currentJvmPipeline.close();
					currentJvmPipeline = null;
				}
				if (currentAndroidPipeline != null) {
					currentAndroidPipeline.close();
					currentAndroidPipeline = null;
				}
			}
		}
	}
}
