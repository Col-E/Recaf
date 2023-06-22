package software.coley.recaf.ui.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableObject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
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

	@Inject
	public WorkspaceExplorerConfig() {
		super(ConfigGroups.SERVICE_UI, "workspace-explorer" + CONFIG_SUFFIX);
		// Add values
		addValue(new BasicConfigValue<>("drag-drop-action", DragDropOption.class, dragDropAction));
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

	public enum DragDropOption {
		CREATE_NEW_WORKSPACE,
		APPEND_TO_CURRENT
	}
}
