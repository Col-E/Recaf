package me.coley.recaf.ui.controls.tree;

import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.util.LangUtil;
import me.coley.recaf.workspace.JavaResource;

import java.io.File;


/**
 * Tree representation of a given {@link JavaResource resource}.
 *
 * @author Matt
 */
@SuppressWarnings("unchecked")
public class ResourceTree extends BorderPane {
	private final GuiController controller;
	private final TextField search;
	private final TreeView tree;

	/**
	 * @param controller
	 * 		Controller to act on.
	 * @param resource
	 * 		Resource to model.
	 */
	public ResourceTree(GuiController controller, JavaResource resource) {
		this.controller = controller;
		// Tree display
		tree = new TreeView();
		tree.setCellFactory(e -> new ResourceCell());
		tree.setRoot(new RootItem(resource));
		tree.getRoot().setExpanded(true);
		tree.setOnMouseClicked(this::onClick);
		tree.setOnKeyPressed(this::onKey);
		tree.setOnDragOver(this::onDragOver);
		tree.setOnDragDropped(this::onDragDrop);
		setCenter(tree);
		// Search field
		search = new TextField();
		search.setPromptText(LangUtil.translate("ui.search") + "...");
		search.getStyleClass().add("search-field");
		search.setOnKeyReleased(e -> {
			// Clear search
			if(e.getCode() == KeyCode.ESCAPE)
				search.setText("");
		});
		search.textProperty().addListener((n, o, v) -> updateSearch(v));
		setBottom(search);
	}

	/**
	 * Filter items in the tree that match <i>(contains)</i> the given text.
	 *
	 * @param text
	 * 		Text to search with.
	 */
	private void updateSearch(String text) {
		RootItem root = (RootItem) tree.getRoot();
		// TODO: More verbose options
		//  - Support for actions, for example:
		//    - "enum:true com/" - search enums in com packages
		//    - "ext:json xyz" - search for whatever ending in ".json"
		root.predicateProperty().set(item -> {
			// Empty predicate -> Simple return.
			if(text.isEmpty())
				return true;
			// Check for content match
			boolean match = false;
			if(item instanceof ClassItem)
				match = ((ClassItem) item).getClassName().contains(text);
			else if(item instanceof ResourceItem)
				match = ((ResourceItem) item).getResourceName().contains(text);
			// Expand items that match, hide those that do not.
			if(match)
				((DirectoryItem) item).expandParents();
			else
				item.setExpanded(false);
			return match;
		});
	}

	private void onClick(MouseEvent e) {
		TreeItem item = (TreeItem) tree.getSelectionModel().getSelectedItem();
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

	private void onKey(KeyEvent e) {
		TreeItem item = (TreeItem) tree.getSelectionModel().getSelectedItem();
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

	private void onDragOver(DragEvent e) {
		// Allow drag-drop content
		if (e.getGestureSource() != tree && e.getDragboard().hasFiles())
			e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
		e.consume();
	}

	private void onDragDrop(DragEvent e) {
		// Load drag-drop files
		if(e.getDragboard().hasFiles()) {
			File file = e.getDragboard().getFiles().get(0);
			controller.loadWorkspace(file);
		}
	}

	/**
	 * Open the class or resource in the ui.
	 *
	 * @param item
	 * 		Item representing value.
	 */
	private void openItem(TreeItem item) {
		if(item instanceof ClassItem) {
			ClassItem ci = (ClassItem) item;
			String name = ci.getClassName();
			JavaResource resource = ci.resource();
			controller.windows().getMainWindow().openClass(resource, name);
		} else if(item instanceof ResourceItem) {
			ResourceItem ri = (ResourceItem) item;
			String name = ri.getResourceName();
			JavaResource resource = ri.resource();
			controller.windows().getMainWindow().openResource(resource, name);
		}
	}

	/**
	 * Opens children recursively as long as only as there is only a path of single children.
	 *
	 * @param item
	 * 		Item to recursively open.
	 */
	private static void recurseOpen(TreeItem item) {
		item.setExpanded(true);
		if(item.getChildren().size() == 1)
			recurseOpen((TreeItem) item.getChildren().get(0));
	}
}
