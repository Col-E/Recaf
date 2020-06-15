package me.coley.recaf.ui.controls;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.tree.*;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.LangUtil;
import me.coley.recaf.workspace.*;

import java.io.File;
import java.util.*;

// TODO: Account for user adds/removes a library to workspace

/**
 * Navigator for a workspace.
 *
 * @author Matt
 */
public class WorkspaceNavigator extends BorderPane {
	private final GuiController controller;
	private final Map<JavaResource, JavaResourceTree> resourceToTree = new HashMap<>();
	private final BorderPane placeholder = new BorderPane();
	private final Label lblPlaceholder = new Label();
	private ResourceComboBox comboResources;

	/**
	 * @param controller
	 * 		Controller.
	 */
	public WorkspaceNavigator(GuiController controller) {
		this.controller = controller;
		// Style as a tree so it takes on the style of what should be there.
		placeholder.getStyleClass().add("tree-view");
		placeholder.setCenter(lblPlaceholder);
		// Create content for workspace
		refresh();
		// Events
		setOnDragOver(this::onDragOver);
		setOnDragDropped(this::onDragDrop);
	}

	/**
	 * Refresh the navigator's content.
	 */
	public void refresh() {
		// Setup trees for each resource
		List<JavaResource> resources = resources();
		if (resources.size() > 1) {
			boolean firstTime = false;
			// Resource switcher
			if (comboResources == null) {
				firstTime = true;
				comboResources = new ResourceComboBox(controller);
				comboResources.getSelectionModel().selectedItemProperty().addListener((v, o, n) -> setCurrent(n));
				BorderPane.setAlignment(comboResources, Pos.CENTER);
				setTop(comboResources);
			}
			// Reset content
			comboResources.getItems().setAll(resources);
			// Select first item if nothing is selected already
			if (firstTime)
				comboResources.getSelectionModel().select(0);
		} else if (controller.getWorkspace() != null) {
			// Only one resource to show
			setCurrent(controller.getWorkspace().getPrimary());
		} else {
			// Set placeholder
			enablePlaceholder();
			clear(LangUtil.translate("ui.looaddrop.prompt"));
		}
	}

	private void setCurrent(JavaResource resource) {
		if (resource != null)
			setCenter(resourceToTree.computeIfAbsent(resource, (k) -> new JavaResourceTree(controller, k)));
	}

	private List<JavaResource> resources() {
		Workspace workspace= controller.getWorkspace();
		if (workspace == null)
			return Collections.emptyList();
		List<JavaResource> list = new ArrayList<>();
		list.add(controller.getWorkspace().getPrimary());
		controller.getWorkspace().getLibraries().stream()
					.filter(res -> !(res instanceof EmptyResource))
					.forEach(list::add);
		return list;
	}

	private void onDragOver(DragEvent e) {
		// Allow drag-drop content
		if (e.getGestureSource() != this && e.getDragboard().hasFiles())
			e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
		e.consume();
	}

	private void onDragDrop(DragEvent e) {
		// Load drag-drop files
		if(e.getDragboard().hasFiles()) {
			File file = e.getDragboard().getFiles().get(0);
			controller.loadWorkspace(IOUtil.toPath(file), null);
		}
	}

	/**
	 * Set the center node to the placeholder.
	 */
	public void enablePlaceholder() {
		setCenter(placeholder);
	}

	/**
	 * Clear display / set placeholder value for center node.
	 *
	 * @param placeholderText
	 * 		Placeholder text to set for center node.
	 */
	public void clear(String placeholderText) {
		lblPlaceholder.setText(placeholderText);
	}



}
