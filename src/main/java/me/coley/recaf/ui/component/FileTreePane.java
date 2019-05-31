package me.coley.recaf.ui.component;

import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import me.coley.event.*;
import me.coley.recaf.config.impl.ConfDisplay;
import me.coley.recaf.event.*;
import me.coley.recaf.Input;
import me.coley.recaf.util.*;
import org.objectweb.asm.tree.*;

/**
 * Pane displaying file-tree of loaded classes.
 * 
 * @author Matt
 */
public class FileTreePane extends BorderPane {
	private final TreeView<String> tree = new TreeView<>();
	private Input input;

	public FileTreePane() {
		Bus.subscribe(this);
		setCenter(tree);
		// drag-drop support for inputs
		tree.setOnDragOver(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent e) {
				if (e.getGestureSource() != tree && e.getDragboard().hasFiles()) {
					e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
				}
				e.consume();
			}
		});
		tree.setOnDragDropped(e -> {
			Dragboard db = e.getDragboard();
			if (db.hasFiles()) {
				NewInputEvent.call(db.getFiles().get(0));
			}
		});
		// Custom tree renderering.
		tree.setShowRoot(false);
		tree.setCellFactory(param -> new TreeCell<String>() {
			@Override
			public void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					// Hide elements.
					// Items enter this state when 'hidden' in the tree.
					setText(null);
					setGraphic(null);
				} else {
					FileTreeItem t = (FileTreeItem) getTreeItem();
					boolean cont = !t.isDirectory() && input.getClasses().containsKey(item);
					ClassNode clazz = cont ? input.getClass(item) : null;
					Node fxImage = cont ? Icons.getClass(clazz.access) : new ImageView(Icons.CL_PACKAGE);
					setGraphic(fxImage);
					String name = cont ? Misc.trim(item, "/") : item;
					int max = ConfDisplay.instance().maxLengthTree;
					if (name.length() > max) {
						name = name.substring(0, max);
					}
					// Append source file name to display if wanted
					if (cont && clazz.sourceFile != null && ConfDisplay.instance().treeSourceNames) {
						name += " (" + clazz.sourceFile + ")";
					}
					setText(name);
				}
			}
		});
		tree.setOnMouseClicked(e -> {
			// Double click to open class
			if (e.getClickCount() == 2) {
				FileTreeItem item = (FileTreeItem) tree.getSelectionModel().getSelectedItem();
				if (item == null) return;

				if (!item.isDirectory()) {
					ClassNode cn = item.get();
					if (cn != null) {
						Bus.post(new ClassOpenEvent(cn));
					}
				} else if (item.isExpanded()) { // when item is expanding
					FileTreeItem currentItem = item;
					// one dir and no file
					while (currentItem.getDirectories().size() == 1 && currentItem.getFiles().isEmpty()) {
						FileTreeItem nextItem = currentItem.getDirectories().values().iterator().next();
						if (!nextItem.isExpanded()) nextItem.setExpanded(true);
						currentItem = nextItem;
					}
				}
			}
		});
		tree.setOnKeyReleased(e -> {
			if (e.getCode() == KeyCode.ENTER) {
				FileTreeItem item = (FileTreeItem) tree.getSelectionModel().getSelectedItem();
				if (item == null || item.isDirectory()) return;
				ClassNode cn = item.get();
				if (cn != null) {
					Bus.post(new ClassOpenEvent(cn));
				}
			}
		});
		Bus.subscribe(this);
		Threads.runFx(() -> tree.requestFocus());
	}

	/**
	 * Resets the tree to match content of input.
	 * 
	 * @param input
	 */
	@Listener
	private void onInputChange(NewInputEvent input) {
		this.input = input.get();
		tree.setRoot(getNodesForDirectory(this.input));
		this.input.registerLoadListener();
	}

	/**
	 * Add new files to the tree as they are loaded.
	 * 
	 * @param load
	 */
	@Listener
	private void onInstrumentedClassLoad(ClassLoadInstrumentedEvent load) {
		FileTreeItem root = (FileTreeItem) tree.getRoot();
		if (root == null) {
			root = new FileTreeItem("root");
			tree.setRoot(root);
		}
		String name = load.getName();
		addToRoot(root, name);
	}

	/**
	 * Move a class being renamed to a new tree-path.
	 * 
	 * @param rename
	 */
	@Listener
	private void onClassRenamed(ClassRenameEvent rename) {
		Threads.runFx(() -> {
			FileTreeItem item = getNode(rename.getOriginalName());
			if (item != null) {
				FileTreeItem parent = (FileTreeItem) item.getParent();
				parent.remove(item);
				addToRoot((FileTreeItem) tree.getRoot(), rename.getNewName());
			}
		});
	}

	/**
	 * Create root for input.
	 * 
	 * @param input
	 * @return {@code FileTreeItem}.
	 */
	private final FileTreeItem getNodesForDirectory(Input input) {
		FileTreeItem root = new FileTreeItem("root");
		input.classes.forEach(name -> {
			addToRoot(root, name);
		});
		return root;
	}

	/**
	 * Add name to root assuming it is loaded in the current Input.
	 * 
	 * @param root
	 *            Root node.
	 * @param name
	 *            Name of class in input.
	 */
	private void addToRoot(FileTreeItem root, String name) {
		FileTreeItem r = root;
		String[] parts = name.split("/");
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			if (i == parts.length - 1) {
				// add final file
				r.addFile(input, part, name);
			} else if (r.hasDir(part)) {
				// navigate to sub-directory
				r = r.getDir(part);
			} else {
				// add sub-dir
				r = r.addDir(part);
			}
		}
	}

	private FileTreeItem getNode(String name) {
		FileTreeItem r = (FileTreeItem) tree.getRoot();
		String[] parts = name.split("/");
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			if (i == parts.length - 1) {
				// get final file
				r = r.getFile(part);
			} else {
				// get sub-dir
				r = r.getDir(part);
			}
		}
		return r;
	}
}
