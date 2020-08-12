package me.coley.recaf.ui.controls.tree;

import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.util.LangUtil;
import me.coley.recaf.workspace.JavaResource;

/**
 * Tree representation of a given {@link JavaResource resource}.
 *
 * @author Matt
 */
@SuppressWarnings("unchecked")
public class JavaResourceTree extends BorderPane {
	private final GuiController controller;
	private final TextField search;
	private final TreeView tree;

	/**
	 * @param controller
	 * 		Controller to act on.
	 * @param resource
	 * 		Resource to model.
	 */
	public JavaResourceTree(GuiController controller, JavaResource resource) {
		this.controller = controller;
		// Tree display
		tree = new TreeView();
		tree.setCellFactory(e -> new JavaResourceCell());
		tree.setRoot(new RootItem(resource));
		tree.getRoot().setExpanded(true);
		tree.setOnMouseClicked(this::onClick);
		tree.setOnKeyPressed(this::onKey);
		setCenter(tree);
		// Search field
		search = new TextField();
		search.setPromptText(LangUtil.translate("ui.search") + "...");
		search.getStyleClass().add("search-field");
		search.setOnKeyReleased(e -> {
			// Clear search
			if(e.getCode() == KeyCode.ESCAPE)
				search.setText("");
			// Navigation keys refocus the tree
			else if (e.getCode() == KeyCode.UP ||
					e.getCode() == KeyCode.DOWN) {
				tree.requestFocus();
			}
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
			else if(item instanceof FileItem)
				match = ((FileItem) item).getFileName().contains(text);
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
			//  (Update context menu builder so source can be used to determine additional options)
			//  - Remove item
			//  - Specifics for classes
			//    - ?
			//  - Specifics for files
			//    - Copy item (to location of given name, prompted)
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
		// Check find keybind
		if (controller.config().keys().find.match(e)) {
			if (search.isFocused())
				search.selectAll();
			search.requestFocus();
			return;
		}
		// Focus text search when typing in tree
		else if (!e.getText().trim().isEmpty()) {
			search.setText(e.getText());
			search.requestFocus();
		}
		// All further actions are for tree-item specific
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
		} else if(item instanceof FileItem) {
			FileItem ri = (FileItem) item;
			String name = ri.getFileName();
			JavaResource resource = ri.resource();
			controller.windows().getMainWindow().openFile(resource, name);
		}
	}

	/**
	 * Opens children recursively as long as only as there is only a path of single children.
	 *
	 * @param item
	 * 		Item to recursively open.
	 */
	public static void recurseOpen(TreeItem item) {
		item.setExpanded(true);
		if(item.getChildren().size() == 1)
			recurseOpen((TreeItem) item.getChildren().get(0));
	}
}
