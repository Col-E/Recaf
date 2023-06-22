package software.coley.recaf.ui.menubar;

import jakarta.annotation.Nonnull;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Menu;
import software.coley.recaf.workspace.WorkspaceCloseListener;
import software.coley.recaf.workspace.WorkspaceManager;
import software.coley.recaf.workspace.WorkspaceOpenListener;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.AgentServerRemoteVmResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * {@link Menu} that is aware of the current workspace environment.
 * <br>
 * Implementations must be scoped beans that provide access to {@link WorkspaceManager}.
 *
 * @author Matt Coley
 */
public abstract class WorkspaceAwareMenu extends Menu implements WorkspaceOpenListener, WorkspaceCloseListener {
	protected final BooleanProperty hasWorkspace = new SimpleBooleanProperty(false);
	protected final BooleanProperty hasAgentWorkspace = new SimpleBooleanProperty(false);

	protected WorkspaceAwareMenu(WorkspaceManager workspaceManager) {
		workspaceManager.addWorkspaceOpenListener(this);
		workspaceManager.addWorkspaceCloseListener(this);
	}

	@Override
	public void onWorkspaceClosed(@Nonnull Workspace workspace) {
		hasWorkspace.set(false);
		hasAgentWorkspace.set(false);
		workspaceStateChanged();
	}

	@Override
	public void onWorkspaceOpened(@Nonnull Workspace workspace) {
		WorkspaceResource primaryResource = workspace.getPrimaryResource();
		hasWorkspace.set(true);
		hasAgentWorkspace.set(primaryResource instanceof AgentServerRemoteVmResource);
		workspaceStateChanged();
	}

	protected void workspaceStateChanged() {
		// no-op by default
	}
}
