package me.coley.recaf.ui.control.tree;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Control;
import javafx.scene.control.TreeView;
import javafx.scene.input.DragEvent;
import me.coley.recaf.RecafUI;
import me.coley.recaf.ui.control.tree.item.BaseTreeValue;
import me.coley.recaf.ui.control.tree.item.RootItem;
import me.coley.recaf.ui.dnd.DragAndDrop;
import me.coley.recaf.ui.dnd.FileDropListener;
import me.coley.recaf.ui.prompt.WorkspaceDropPrompts;
import me.coley.recaf.util.Threads;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;

import java.nio.file.Path;
import java.util.List;

/**
 * Tree view of a {@link Workspace}.
 *
 * @author Matt Coley
 */
public class WorkspaceTree extends TreeView<BaseTreeValue> implements FileDropListener {
	private final SimpleBooleanProperty hideLibrarySubElements = new SimpleBooleanProperty();
	private final SimpleBooleanProperty caseSensitivity = new SimpleBooleanProperty();
	private Workspace workspace;

	/**
	 * Initialize the workspace tree.
	 */
	public WorkspaceTree() {
		setShowRoot(false);
		setCellFactory(new WorkspaceCellFactory());
		setWorkspace(RecafUI.getController().getWorkspace());
		getStyleClass().add("workspace-tree");
		// Add drag-and-drop support
		DragAndDrop.installFileSupport(this, this);
	}

	@Override
	public void onDragDrop(Control control, DragEvent event, List<Path> files) {
		// Run on a separate thread so we don't have the dragged files locked on the user's screen
		Threads.runFx(() -> {
			if (workspace == null) {
				Workspace created = WorkspaceDropPrompts.createWorkspace(files);
				if (created != null) {
					RecafUI.getController().setWorkspace(created);
				}
				return;
			}
			// TODO: May want to add a config option to always do one action
			// Check what the user wants to do with these files
			WorkspaceDropPrompts.WorkspaceDropResult result = WorkspaceDropPrompts.prompt(files);
			switch (result.getAction()) {
				case ADD_TO_WORKSPACE:
					// Users chose to add files to workspace as library resources
					result.getLibraries().forEach(library -> workspace.addLibrary(library));
					break;
				case CREATE_NEW_WORKSPACE:
					// Users chose to make new workspace from dropped file(s)
					RecafUI.getController().setWorkspace(result.getWorkspace());
					break;
				case CANCEL:
				default:
					// Users chose to cancel
			}
		});
	}

	/**
	 * @return {@code true} when the tree is hiding library resources.
	 * {@code false} when all items are shown.
	 */
	public boolean isHideLibrarySubElements() {
		return hideLibrarySubElements.get();
	}

	/**
	 * @return {@code true} when the {@link me.coley.recaf.ui.control.WorkspaceFilterField} name search should be
	 * considered as case-sensitive. {@code false} for insensitivity.
	 */
	public boolean isCaseSensitive() {
		return caseSensitivity.get();
	}

	/**
	 * @return Property of {@link #isHideLibrarySubElements()}.
	 */
	public SimpleBooleanProperty hideLibrarySubElementsProperty() {
		return hideLibrarySubElements;
	}

	/**
	 * @return Property of {@link #isCaseSensitive()}.
	 */
	public SimpleBooleanProperty caseSensitiveProperty() {
		return caseSensitivity;
	}

	/**
	 * Toggle display of library items.
	 */
	public void toggleHideLibraries() {
		setHideLibraries(!hideLibrarySubElements.get());
	}

	/**
	 * Toggle {@link me.coley.recaf.ui.control.WorkspaceFilterField} name search's case sensitivity.
	 */
	public void toggleCaseSensitivity() {
		setCaseSensitivity(!caseSensitivity.get());
	}

	/**
	 * @param hideLibrarySubElements
	 * 		New hide option value.
	 */
	public void setHideLibraries(boolean hideLibrarySubElements) {
		this.hideLibrarySubElements.set(hideLibrarySubElements);
		onUpdateHiddenLibrarySubElements();
	}

	/**
	 * @param caseSensitivity
	 * 		New sensitivity option.
	 */
	public void setCaseSensitivity(boolean caseSensitivity) {
		this.caseSensitivity.set(caseSensitivity);
	}

	/**
	 * @return Associated workspace.
	 */
	public Workspace getWorkspace() {
		return workspace;
	}

	/**
	 * Load the given workspace.
	 *
	 * @param workspace
	 * 		Workspace to represent in the tree.
	 */
	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
		// Set new root item
		RootItem root = new RootItem(workspace);
		root.setup();
		// Updating root must be on UI thread
		Threads.runFx(() -> {
			setRoot(root);
			getRoot().setExpanded(true);
		});
		// FYI: We don't need to clean up any listener stuff in here
		// since the cleanup process for workspaces themselves will
		// handle that for us.
	}

	/**
	 * Utility cast.
	 *
	 * @return {@link #getRoot()} casted to {@link RootItem}.
	 */
	public RootItem getRootItem() {
		return (RootItem) super.getRoot();
	}

	/**
	 * Updates the tree nodes to hide library-sub-elements and disable searching of the hidden elements.
	 */
	private void onUpdateHiddenLibrarySubElements() {
		// Skip if no workspace open
		if (workspace == null) {
			return;
		}
		// Add or remove libraries based on hide flag
		for (Resource library : workspace.getResources().getLibraries()) {
			// Note: this is probably not the most efficient way to do this since
			// this will cause the tree to regenerate the nodes for resources.
			// Its nothing major but can probably be improved later if performance is an issue
			// on large workspaces.
			if (hideLibrarySubElements.get()) {
				getRootItem().removeResource(library);
			} else {
				getRootItem().addResource(library);
			}
		}
	}
}
