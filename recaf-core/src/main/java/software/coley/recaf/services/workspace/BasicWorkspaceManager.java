package software.coley.recaf.services.workspace;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.workspace.model.EmptyWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.WorkspaceModificationListener;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Basic workspace manager implementation.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicWorkspaceManager implements WorkspaceManager {
	private static final Logger logger = Logging.get(BasicWorkspaceManager.class);
	private final List<WorkspaceCloseCondition> closeConditions = new CopyOnWriteArrayList<>();
	private final List<WorkspaceOpenListener> openListeners = new CopyOnWriteArrayList<>();
	private final List<WorkspaceCloseListener> closeListeners = new CopyOnWriteArrayList<>();
	private final List<WorkspaceModificationListener> defaultModificationListeners = new CopyOnWriteArrayList<>();
	private final WorkspaceManagerConfig config;
	private Workspace current;

	@Inject
	public BasicWorkspaceManager(@Nonnull WorkspaceManagerConfig config) {
		this.config = config;
	}

	@Override
	@Produces
	@Dependent
	public Workspace getCurrent() {
		if (current == null)
			return EmptyWorkspace.get();
		return current;
	}

	@Override
	public void setCurrentIgnoringConditions(Workspace workspace) {
		Workspace currentWorkspace = current;
		if (currentWorkspace != null) {
			currentWorkspace.close();
			Unchecked.checkedForEach(closeListeners, listener -> listener.onWorkspaceClosed(currentWorkspace),
					(listener, t) -> logger.error("Exception thrown when closing workspace", t));
		}
		current = workspace;
		if (workspace != null) {
			defaultModificationListeners.forEach(workspace::addWorkspaceModificationListener);
			Unchecked.checkedForEach(openListeners, listener -> listener.onWorkspaceOpened(workspace),
					(listener, t) -> logger.error("Exception thrown by when opening workspace", t));
		}
	}

	@Nonnull
	@Override
	public List<WorkspaceCloseCondition> getWorkspaceCloseConditions() {
		return closeConditions;
	}

	@Override
	public void addWorkspaceCloseCondition(@Nonnull WorkspaceCloseCondition condition) {
		closeConditions.add(condition);
	}

	@Override
	public void removeWorkspaceCloseCondition(@Nonnull WorkspaceCloseCondition condition) {
		closeConditions.remove(condition);
	}

	@Nonnull
	@Override
	public List<WorkspaceOpenListener> getWorkspaceOpenListeners() {
		return openListeners;
	}

	@Override
	public void addWorkspaceOpenListener(@Nonnull WorkspaceOpenListener listener) {
		openListeners.add(listener);
	}

	@Override
	public void removeWorkspaceOpenListener(@Nonnull WorkspaceOpenListener listener) {
		openListeners.remove(listener);
	}

	@Nonnull
	@Override
	public List<WorkspaceCloseListener> getWorkspaceCloseListeners() {
		return closeListeners;
	}

	@Override
	public void addWorkspaceCloseListener(@Nonnull WorkspaceCloseListener listener) {
		closeListeners.add(listener);
	}

	@Override
	public void removeWorkspaceCloseListener(@Nonnull WorkspaceCloseListener listener) {
		closeListeners.remove(listener);
	}

	@Nonnull
	@Override
	public List<WorkspaceModificationListener> getDefaultWorkspaceModificationListeners() {
		return defaultModificationListeners;
	}

	@Override
	public void addDefaultWorkspaceModificationListeners(@Nonnull WorkspaceModificationListener listener) {
		defaultModificationListeners.add(listener);
	}

	@Override
	public void removeDefaultWorkspaceModificationListeners(@Nonnull WorkspaceModificationListener listener) {
		defaultModificationListeners.remove(listener);

		addDefaultWorkspaceModificationListeners(new WorkspaceModificationListener() {
			@Override
			public void onAddLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
				// Supporting library added to workspace
			}

			@Override
			public void onRemoveLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
				// Supporting library removed from workspace
			}
		});
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public WorkspaceManagerConfig getServiceConfig() {
		return config;
	}
}
