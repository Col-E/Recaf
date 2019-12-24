package me.coley.recaf.ui.controls;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.tree.*;
import me.coley.recaf.util.LangUtil;
import me.coley.recaf.util.UiUtil;
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
	private final Map<JavaResource, ResourceTree> resourceToTree = new HashMap<>();
	private final BorderPane placeholder = new BorderPane();
	private final Label lblPlaceholder = new Label();

	/**
	 * @param controller
	 * 		Controller.
	 */
	public WorkspaceNavigator(GuiController controller) {
		this.controller = controller;
		// Style as a tree so it takes on the style of what should be there.
		placeholder.getStyleClass().add("tree-view");
		placeholder.setCenter(lblPlaceholder);
		// Setup trees for each resource
		List<JavaResource> resources = resources();
		if (resources.size() > 1) {
			// Resource switcher
			ComboBox<JavaResource> comboResources = new ComboBox<>();
			comboResources.getStyleClass().add("resource-selector");
			comboResources.getItems().addAll(resources);
			comboResources.getSelectionModel().selectedItemProperty().addListener((v, o, n) -> setCurrent(n));
			comboResources.getSelectionModel().select(0);
			comboResources.setMaxWidth(Double.MAX_VALUE);
			comboResources.setCellFactory(e -> new ResourceSelectionCell());
			BorderPane.setAlignment(comboResources, Pos.CENTER);
			setTop(comboResources);
		} else if (controller.getWorkspace() != null) {
			// Only one resource to show
			setCurrent(controller.getWorkspace().getPrimary());
		} else {
			// Set placeholder
			clear(LangUtil.translate("ui.looaddrop.prompt"));
		}
		// Events
		setOnDragOver(this::onDragOver);
		setOnDragDropped(this::onDragDrop);
	}

	private void setCurrent(JavaResource resource) {
		setCenter(resourceToTree.computeIfAbsent(resource, (k) -> new ResourceTree(controller, k)));
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
			controller.loadWorkspace(file, null);
		}
	}

	/**
	 * Clear display / set placeholder value for center node.
	 *
	 * @param placeholderText
	 * 		Placeholder text to set for center node.
	 */
	public void clear(String placeholderText) {
		lblPlaceholder.setText(placeholderText);
		setCenter(placeholder);
	}

	/**
	 * Cell/renderer for displaying {@link JavaResource}s.
	 */
	private class ResourceSelectionCell extends ComboBoxListCell<JavaResource> {
		@Override
		public void updateItem(JavaResource item, boolean empty) {
			super.updateItem(item, empty);
			if(!empty) {
				HBox g = new HBox();
				if(item != null) {
					String t = item.toString();
					// Add icon for resource types
					g.getChildren().add(new IconView(UiUtil.getResourceIcon(item)));
					// Indicate which resource is the primary resource
					if(item == controller.getWorkspace().getPrimary()) {
						Label lbl = new Label(" [Primary]");
						lbl.getStyleClass().add("bold");
						g.getChildren().add(lbl);
					}
					setText(t);
				}
				setGraphic(g);
			} else {
				setGraphic(null);
				setText(null);
			}
		}
	}
}
