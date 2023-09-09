package software.coley.recaf.ui.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableInteger;
import software.coley.observables.ObservableObject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.ui.control.tree.WorkspaceTree;
import software.coley.recaf.ui.pane.WorkspaceExplorerPane;

/**
 * Config for workspace explorer.
 *
 * @author Matt Coley
 * @see WorkspaceExplorerPane
 */
@ApplicationScoped
public class WorkspaceExplorerConfig extends BasicConfigContainer {
	private final ObservableObject<DragDropOption> dragDropAction = new ObservableObject<>(DragDropOption.CREATE_NEW_WORKSPACE);
	private final ObservableInteger maxTreeDirectoryDepth = new ObservableInteger(100);

	@Inject
	public WorkspaceExplorerConfig() {
		super(ConfigGroups.SERVICE_UI, "workspace-explorer" + CONFIG_SUFFIX);
		// Add values
		addValue(new BasicConfigValue<>("drag-drop-action", DragDropOption.class, dragDropAction));
		addValue(new BasicConfigValue<>("max-tree-dir-depth", int.class, maxTreeDirectoryDepth));
	}

	/**
	 * @return {@code true} when drag-drop behavior on {@link WorkspaceExplorerPane} should create new workspaces.
	 */
	public boolean createOnDragDrop() {
		return dragDropAction.getValue() == DragDropOption.CREATE_NEW_WORKSPACE;
	}

	/**
	 * @return {@code true} when drag-drop behavior on {@link WorkspaceExplorerPane} should append files to the open workspace.
	 */
	public boolean appendOnDragDrop() {
		return dragDropAction.getValue() == DragDropOption.APPEND_TO_CURRENT;
	}

	/**
	 * @return Number of directories deep to start compacting paths in the {@link WorkspaceTree}.
	 */
	public int getMaxTreeDirectoryDepth() {
		return Math.max(1, maxTreeDirectoryDepth.getValue());
	}

	public enum DragDropOption {
		CREATE_NEW_WORKSPACE,
		APPEND_TO_CURRENT
	}
}
