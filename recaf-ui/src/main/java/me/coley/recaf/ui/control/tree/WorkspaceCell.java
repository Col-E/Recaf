package me.coley.recaf.ui.control.tree;

import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import me.coley.recaf.RecafUI;
import me.coley.recaf.ui.control.tree.item.*;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.DexClassInfo;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static me.coley.recaf.ui.util.Icons.getClassIcon;
import static me.coley.recaf.ui.util.Icons.getIconView;

/**
 * Cell for {@link BaseTreeValue} to represent items from a {@link Workspace}.
 *
 * @author Matt Coley
 */
public class WorkspaceCell extends TreeCell<BaseTreeValue> {
	private static final Logger logger = Logging.get(WorkspaceCell.class);
	private static final Map<Class<?>, BiFunction<Workspace, BaseTreeValue, String>> TEXT_FUNCS = new HashMap<>();
	private static final Map<Class<?>, BiFunction<Workspace, BaseTreeValue, Node>> GRAPHIC_FUNCS = new HashMap<>();

	/**
	 * Create a new cell.
	 */
	public WorkspaceCell() {
		setOnMouseClicked(this::onMouseClick);
		// TODO: Context menu items once centralized context system is set-up
	}

	private void onMouseClick(MouseEvent e) {
		TreeItem<?> item = getTreeItem();
		if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
			// Open selected
			if (item.isLeaf()) {
				openItem(item);
			}
			// Recursively open children until multiple options are present
			else if (item.isExpanded()) {
				BaseTreeItem.recurseOpen(item);
			}
		}
	}

	/**
	 * Open the content of the tree item in the UI.
	 *
	 * @param item
	 * 		Item representing value.
	 */
	private void openItem(TreeItem<?> item) {
		// TODO: Open classes/files
		/*
		if (item instanceof ClassItem) {
			ClassItem ci = (ClassItem) item;
			String name = ci.getClassName();
			// CLASS OPEN HERE
		} else if (item instanceof DexClassItem) {
			DexClassItem dci = (DexClassItem) item;
			String name = dci.getClassName();
			// CLASS OPEN HERE
		} else if (item instanceof FileItem) {
			FileItem ri = (FileItem) item;
			String name = ri.getFileName();
			// FILE OPEN HERE
		}
		*/
	}

	@Override
	protected void updateItem(BaseTreeValue value, boolean empty) {
		super.updateItem(value, empty);
		if (empty) {
			setGraphic(null);
			setText(null);
			setContextMenu(null);
		} else {
			setText(getValueText(value));
			setGraphic(getValueGraphic(value));
		}
	}

	private String getValueText(BaseTreeValue value) {
		BaseTreeItem item = value.getItem();
		// Apply function if available
		BiFunction<Workspace, BaseTreeValue, String> textFunction = TEXT_FUNCS.get(item.getClass());
		if (textFunction != null) {
			return textFunction.apply(workspace(), value);
		}
		// Just use element name.
		return value.getPathElementValue();
	}

	private Node getValueGraphic(BaseTreeValue value) {
		BaseTreeItem item = value.getItem();
		// Apply function if available
		BiFunction<Workspace, BaseTreeValue, Node> graphicFunction = GRAPHIC_FUNCS.get(item.getClass());
		if (graphicFunction != null) {
			return graphicFunction.apply(workspace(), value);
		}
		// No graphic
		return null;
	}

	/**
	 * Static accessor to current workspace.
	 * Used because cells are reused and cannot be given a per-instance workspace.
	 *
	 * @return Current workspace.
	 */
	private static Workspace workspace() {
		return RecafUI.getController().getWorkspace();
	}

	static {
		// Text
		TEXT_FUNCS.put(RootItem.class, (w, v) -> "Root");
		TEXT_FUNCS.put(DummyItem.class, (w, v) -> ((DummyItem) v.getItem()).getDummyText());
		TEXT_FUNCS.put(ResourceItem.class, (w, v) ->
				((ResourceItem) v.getItem()).getResource().getContentSource().toString());
		TEXT_FUNCS.put(ResourceClassesItem.class, (w, v) -> Lang.get("tree.classes"));
		TEXT_FUNCS.put(ResourceFilesItem.class, (w, v) -> Lang.get("tree.files"));
		TEXT_FUNCS.put(ResourceDexClassesItem.class, (w, v) -> ((ResourceDexClassesItem) v.getItem()).getDexName());
		// Icons
		GRAPHIC_FUNCS.put(ResourceItem.class, (w, v) -> {
			ResourceItem resourceItem = (ResourceItem) v.getItem();
			return Icons.getResourceIcon(resourceItem.getResource());
		});
		GRAPHIC_FUNCS.put(ResourceClassesItem.class, (w, v) -> getIconView(Icons.FOLDER_SRC));
		GRAPHIC_FUNCS.put(ResourceDexClassesItem.class, (w, v) -> getIconView(Icons.FOLDER_SRC));
		GRAPHIC_FUNCS.put(ResourceFilesItem.class, (w, v) -> getIconView(Icons.FOLDER_RES));
		GRAPHIC_FUNCS.put(PackageItem.class, (w, v) -> getIconView(Icons.FOLDER_PACKAGE));
		GRAPHIC_FUNCS.put(DirectoryItem.class, (w, v) -> getIconView(Icons.FOLDER));
		GRAPHIC_FUNCS.put(ClassItem.class, (w, v) -> {
			String className = ((ClassItem) v.getItem()).getClassName();
			ClassInfo info = w.getResources().getClass(className);
			if (info == null) {
				logger.error("Failed to lookup class for tree cell '{}'", className);
				return getIconView(Icons.CLASS);
			}
			// TODO: Cleanup access usage once access utility is added to project
			return getClassIcon(info);
		});
		GRAPHIC_FUNCS.put(DexClassItem.class, (w, v) -> {
			String className = ((DexClassItem) v.getItem()).getClassName();
			DexClassInfo info = w.getResources().getDexClass(className);
			if (info == null) {
				logger.error("Failed to lookup dex class for tree cell '{}'", className);
				return getIconView(Icons.CLASS);
			}
			return getClassIcon(info);
		});
		GRAPHIC_FUNCS.put(FileItem.class, (w, v) -> {
			// TODO: Determine file type
			return getIconView(Icons.FILE_BINARY);
		});
	}
}
