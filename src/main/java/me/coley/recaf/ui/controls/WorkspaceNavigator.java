package me.coley.recaf.ui.controls;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.tree.*;
import me.coley.recaf.workspace.*;

import java.util.*;

/* TODO: Account for the following
 *  - Search bar @ bottom (Keybind: Find) to show
 *  - User adds/removes a library to workspace
 *  - User adds/remove/renames a class/resource
 *  - User drops file on space
 *    - Loads new workspace
 */

/**
 * Navigator for a workspace.
 *
 * @author Matt
 */
public class WorkspaceNavigator extends BorderPane {
	private final GuiController controller;
	private final Map<JavaResource, ResourceTree> resourceToTree = new HashMap<>();

	/**
	 * @param controller
	 * 		Controller.
	 */
	public WorkspaceNavigator(GuiController controller) {
		this.controller = controller;
		ComboBox<JavaResource> comboResources = new ComboBox<>();
		comboResources.getItems().addAll(resources());
		comboResources.getSelectionModel().selectedItemProperty().addListener((v, o, n) -> setCurrent(n));
		comboResources.getSelectionModel().select(0);
		comboResources.setMaxWidth(Double.MAX_VALUE);
		comboResources.setCellFactory(e -> new XCell());
		BorderPane.setAlignment(comboResources, Pos.CENTER);
 		setTop(comboResources);
	}

	private void setCurrent(JavaResource resource) {
		setCenter(resourceToTree.computeIfAbsent(resource, (k) -> new ResourceTree(controller, k)));
	}

	private List<JavaResource> resources() {
		List<JavaResource> list = new ArrayList<>();
		list.add(controller.getWorkspace().getPrimary());
		list.addAll(controller.getWorkspace().getLibraries());
		return list;
	}

	private class XCell extends ComboBoxListCell<JavaResource> {
		@Override
		public void updateItem(JavaResource item, boolean empty) {
			super.updateItem(item, empty);
			if(!empty) {
				HBox g = new HBox();
				if(item != null) {
					String t = item.toString();
					// Add icon for resource types
					if(item instanceof JarResource) {
						g.getChildren().add(new IconView("icons/jar.png"));
					} else if(item instanceof ClassResource) {
						g.getChildren().add(new IconView("icons/binary.png"));
					} else if(item instanceof UrlResource) {
						g.getChildren().add(new IconView("icons/link.png"));
					} else if(item instanceof MavenResource) {
						g.getChildren().add(new IconView("icons/data.png"));
					}
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
