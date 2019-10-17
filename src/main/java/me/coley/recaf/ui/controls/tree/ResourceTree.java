package me.coley.recaf.ui.controls.tree;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.*;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.workspace.JavaResource;

/**
 * Tree representation of a given {@link JavaResource resource}.
 *
 * @author Matt
 */
@SuppressWarnings("unchecked")
public class ResourceTree extends TreeView {
	private final GuiController controller;

	public ResourceTree(GuiController controller, JavaResource resource) {
		this.controller = controller;
		setCellFactory(e -> new ResourceCell());
		setRoot(new RootItem(resource));
		getRoot().setExpanded(true);
		setOnMouseClicked(this::click);
		setOnKeyReleased(this::key);
	}

	private void click(MouseEvent e) {
		TreeItem item = (TreeItem) getSelectionModel().getSelectedItem();
		if(item == null)
			return;
		// Right click
		if(e.getButton() == MouseButton.SECONDARY) {
			// TODO: Context menu items
			//  - Remove item
			//  - Copy item (to location of given name, prompted)
			//  - Specifics for classes
			//    - Search for references to the class
			//    - ?
			//  - Specifics for resources
			//    - ?
		}
		// Double click
		else if(e.getClickCount() == 2) {
			// Open selected
			if(item.isLeaf())
				openItem(item);
				// Recursively open children until multiple options are present
			else if(item.isExpanded()) {
				recurseOpen(item);
			}
		}
	}

	private void key(KeyEvent e) {
		TreeItem item = (TreeItem) getSelectionModel().getSelectedItem();
		if(item == null)
			return;
		// Open selected
		if(e.getCode() == KeyCode.ENTER) {
			if(!item.isLeaf())
				return;
			openItem(item);
		}
		// Recursively open children until multiple options are present
		else if(e.getCode() == KeyCode.RIGHT || e.getCode() == KeyCode.KP_RIGHT) {
			recurseOpen(item);
		}
	}

	private void openItem(TreeItem item) {
		// TODO: Handle opening via controller
		if(item instanceof ClassItem) {

		} else if(item instanceof ResourceItem) {

		}
	}

	private void recurseOpen(TreeItem item) {
		item.setExpanded(true);
		if(item.getChildren().size() == 1) {
			recurseOpen((TreeItem) item.getChildren().get(0));
		}
	}
}
