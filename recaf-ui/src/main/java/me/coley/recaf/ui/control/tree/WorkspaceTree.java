package me.coley.recaf.ui.control.tree;

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

import java.nio.file.Path;
import java.util.List;

/**
 * Tree view of a {@link Workspace}.
 *
 * @author Matt Coley
 */
public class WorkspaceTree extends TreeView<BaseTreeValue> implements FileDropListener {
	private Workspace workspace;
	private boolean hideLibrarySubElements;

	/**
	 * Initialize the workspace tree.
	 */
	public WorkspaceTree() {
		setShowRoot(false);
		setCellFactory(new WorkspaceCellFactory());
		setWorkspace(RecafUI.getController().getWorkspace());
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
	 * @param hideLibrarySubElements
	 * 		New hide option value.
	 */
	public void setHideLibrarySubElements(boolean hideLibrarySubElements) {
		this.hideLibrarySubElements = hideLibrarySubElements;
		onUpdateHiddenLibrarySubElements();
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
		// TODO: Update tree
		//  - Hide library sub-elements
		//  - Disable search under library elements
	}
}
