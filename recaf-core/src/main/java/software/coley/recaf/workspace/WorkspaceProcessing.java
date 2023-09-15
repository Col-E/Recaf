package software.coley.recaf.workspace;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.cdi.EagerInitialization;
import software.coley.recaf.cdi.WorkspaceScoped;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Applies all discovered {@link WorkspaceProcessor} instances to {@link Workspace} instances upon loading them via
 * {@link WorkspaceManager#setCurrent(Workspace)}.
 *
 * @author Matt Coley
 * @see WorkspaceProcessor Processor type to implement.
 */
@WorkspaceScoped
@EagerInitialization
public class WorkspaceProcessing {
	private static final Logger logger = Logging.get(WorkspaceProcessing.class);

	/**
	 * Since this is an eager <i>({@link EagerInitialization})</i> {@link WorkspaceScoped} bean a new instance is created
	 * every time a workspace is opened. Since it takes in all discovered {@link WorkspaceProcessor} via the {@code processors}
	 * parameter, all processors are applied to the given workspace.
	 * <br>
	 * Any implementation of {@link WorkspaceProcessor} can thus be either a {@link ApplicationScoped} or {@link WorkspaceScoped}
	 * bean. The benefit of this is that {@link WorkspaceScoped} beans can {@link Inject} other {@link WorkspaceScoped} services
	 * such as {@link InheritanceGraph}.
	 *
	 * @param workspace
	 * 		Opened workspace.
	 * @param processors
	 * 		Discovered processors to apply.
	 */
	@Inject
	public WorkspaceProcessing(@Nonnull Workspace workspace, @Nonnull Instance<WorkspaceProcessor> processors) {
		for (WorkspaceProcessor processor : processors) {
			logger.info("Applying workspace processor: {}", processor.name());
			processor.onWorkspaceOpened(workspace);
		}
	}
}
