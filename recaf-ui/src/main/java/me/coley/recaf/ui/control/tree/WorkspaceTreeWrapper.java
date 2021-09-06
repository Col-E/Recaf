package me.coley.recaf.ui.control.tree;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TreeView;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import me.coley.recaf.RecafUI;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.control.tree.item.WorkspaceRootItem;
import me.coley.recaf.ui.dnd.DragAndDrop;
import me.coley.recaf.ui.dnd.FileDropListener;
import me.coley.recaf.ui.prompt.WorkspaceDropPrompts;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.util.Threads;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;

import java.nio.file.Path;
import java.util.List;

/**
 * Wrapper around a tree view of a {@link Workspace}.
 *
 * @author Matt Coley
 */
public class WorkspaceTreeWrapper extends StackPane implements FileDropListener {
	private final WorkspaceTree tree = new WorkspaceTree(WorkspaceTreeType.WORKSPACE_NAVIGATION);
	private final SimpleBooleanProperty hideLibrarySubElements = new SimpleBooleanProperty();
	private final SimpleBooleanProperty caseSensitivity = new SimpleBooleanProperty();
	private Node overlay;
	private Workspace workspace;

	/**
	 * Initialize the workspace tree.
	 */
	public WorkspaceTreeWrapper() {
		tree.setShowRoot(false);
		getChildren().add(tree);
		setWorkspace(RecafUI.getController().getWorkspace());
		getStyleClass().add("workspace-tree");
		// Add drag-and-drop support
		DragAndDrop.installFileSupport(this, this);
	}

	@Override
	public void onDragDrop(Region region, DragEvent event, List<Path> files) {
		Threads.run(() -> {
			// Update overlay
			addLoadingOverlay(files);
			// Read files, this is the slow part that is why we run this on a separate thread
			List<Resource> resources = WorkspaceDropPrompts.readResources(files);
			// Check for initial case
			if (workspace == null) {
				Threads.runFx(() -> {
					Workspace created = WorkspaceDropPrompts.createWorkspace(resources);
					if (created != null) {
						RecafUI.getController().setWorkspace(created);
					}
				});
				// Remove overlay
				clearOverlay();
				return;
			}
			Threads.runFx(() -> {
				// Check what the user wants to do with these files
				WorkspaceDropPrompts.WorkspaceDropResult result;
				switch (Configs.display().onFileDrop) {
					default:
					case CHOOSE:
						result = WorkspaceDropPrompts.prompt(resources);
						break;
					case CREATE_NEW:
						result = WorkspaceDropPrompts.workspace(WorkspaceDropPrompts.createWorkspace(resources));
						break;
					case ADD_LIBRARY:
						result = WorkspaceDropPrompts.add(resources);
						break;
				}
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
			// Remove overlay
			clearOverlay();
		});
	}

	/**
	 * Populate overlay with loading information.
	 *
	 * @param files
	 * 		Files being loaded.
	 */
	private void addLoadingOverlay(List<Path> files) {
		VBox centeredList = new VBox();
		centeredList.setFillWidth(false);
		centeredList.setSpacing(5);
		centeredList.setAlignment(Pos.CENTER);
		VBox fileNameWrapper = new VBox();
		fileNameWrapper.setAlignment(Pos.CENTER_LEFT);
		for (Path path : files) {
			Label label = new Label(path.getFileName().toString());
			label.setGraphic(Icons.getPathIcon(path));
			fileNameWrapper.getChildren().add(label);
		}
		centeredList.getChildren().add(new Label(String.format("Reading %d files:", files.size())));
		centeredList.getChildren().add(fileNameWrapper);
		BorderPane pane = new BorderPane();
		centeredList.getStyleClass().add("workspace-overlay");
		pane.setCenter(centeredList);
		overlay = pane;
		Threads.runFx(() -> {
			tree.setEffect(new GaussianBlur());
			tree.setDisable(true);
			getChildren().add(overlay);
		});
	}

	/**
	 * Clear any overlay item.
	 */
	public void clearOverlay() {
		Threads.runFx(() -> {
			tree.setEffect(null);
			tree.setDisable(false);
			while (getChildren().size() > 1) {
				getChildren().remove(1);
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
		WorkspaceRootItem root = new WorkspaceRootItem(workspace);
		root.setup();
		// Updating root must be on UI thread
		Threads.runFx(() -> {
			tree.setRoot(root);
			tree.getRoot().setExpanded(true);
			tree.getSelectionModel().select(0);
			tree.requestFocus();
		});
		// FYI: We don't need to clean up any listener stuff in here
		// since the cleanup process for workspaces themselves will
		// handle that for us.
	}

	/**
	 * Utility cast.
	 *
	 * @return The {@link TreeView#getRoot()} of the {@link #tree} casted to {@link WorkspaceRootItem}.
	 */
	public WorkspaceRootItem getRootItem() {
		return (WorkspaceRootItem) tree.getRoot();
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
