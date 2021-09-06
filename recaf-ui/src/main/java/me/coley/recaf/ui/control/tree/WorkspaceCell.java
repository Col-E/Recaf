package me.coley.recaf.ui.control.tree;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.*;
import me.coley.recaf.ui.context.ContextBuilder;
import me.coley.recaf.ui.context.ContextSource;
import me.coley.recaf.ui.control.tree.item.*;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resources;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static me.coley.recaf.ui.util.Icons.*;

/**
 * Cell for {@link BaseTreeValue} to represent items from a {@link Workspace}.
 *
 * @author Matt Coley
 */
public class WorkspaceCell extends TreeCell<BaseTreeValue> {
	private static final Logger logger = Logging.get(WorkspaceCell.class);
	private static final Map<Class<?>, BiFunction<Workspace, BaseTreeValue, String>> TEXT_FUNCS = new HashMap<>();
	private static final Map<Class<?>, BiFunction<Workspace, BaseTreeValue, Node>> GRAPHIC_FUNCS = new HashMap<>();
	private static final Map<Class<?>, BiFunction<Workspace, BaseTreeValue, ContextBuilder>> CONTEXT_FUNCS =
			new HashMap<>();
	private final WorkspaceTreeType treeType;

	/**
	 * Create a new cell.
	 *
	 * @param treeType
	 * 		Parent tree's type. May result in changes in context menu option availability.
	 */
	public WorkspaceCell(WorkspaceTreeType treeType) {
		this.treeType = treeType;
	}

	private void onMouseClick(MouseEvent e) {
		TreeItem<?> item = getTreeItem();
		if (item == null) {
			logger.error("Failed to get tree item of current cell: {}", getText());
			return;
		}
		if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
			// Open selected
			if (item.isLeaf()) {
				WorkspaceTree.openItem(item);
			}
			// Recursively open children until multiple options are present
			else if (item.isExpanded()) {
				BaseTreeItem.recurseOpen(item);
			}
		}
	}

	@Override
	protected void updateItem(BaseTreeValue value, boolean empty) {
		super.updateItem(value, empty);
		if (empty) {
			setGraphic(null);
			setText(null);
			setContextMenu(null);
			setOnMouseClicked(null);
		} else {
			setText(getValueText(value));
			setGraphic(getValueGraphic(value));
			setContextMenu(getContextMenu(value, treeType));
			setOnMouseClicked(this::onMouseClick);
		}
	}

	private static String getValueText(BaseTreeValue value) {
		BaseTreeItem item = value.getItem();
		// Apply function if available
		BiFunction<Workspace, BaseTreeValue, String> textFunction = TEXT_FUNCS.get(item.getClass());
		if (textFunction != null) {
			return textFunction.apply(workspace(), value);
		}
		// Just use element name.
		return value.getPathElementValue();
	}

	private static Node getValueGraphic(BaseTreeValue value) {
		BaseTreeItem item = value.getItem();
		// Apply function if available
		BiFunction<Workspace, BaseTreeValue, Node> graphicFunction = GRAPHIC_FUNCS.get(item.getClass());
		if (graphicFunction != null) {
			return graphicFunction.apply(workspace(), value);
		}
		// No graphic
		return null;
	}

	private static ContextMenu getContextMenu(BaseTreeValue value, WorkspaceTreeType treeType) {
		BaseTreeItem item = value.getItem();
		// Apply function if available
		BiFunction<Workspace, BaseTreeValue, ContextBuilder> contextMenuFunction = CONTEXT_FUNCS.get(item.getClass());
		if (contextMenuFunction != null) {
			ContextBuilder builder = contextMenuFunction.apply(workspace(), value);
			switch (treeType) {
				default:
				case WORKSPACE_NAVIGATION:
					builder.setWhere(ContextSource.WORKSPACE_TREE);
					break;
				case SEARCH_RESULTS:
					builder.setWhere(ContextSource.SEARCH_RESULTS);
					break;
			}
			return builder.build();
		}
		// No menu
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
		TEXT_FUNCS.put(WorkspaceRootItem.class, (w, v) -> "Root");
		TEXT_FUNCS.put(ResultsRootItem.class, (w, v) -> {
			ResultsRootItem i = (ResultsRootItem) v.getItem();
			return String.format("%s - %d results", i.getSearch().toString(), i.getResults().size());
		});
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
		GRAPHIC_FUNCS.put(FieldItem.class, (w, v) -> {
			FieldInfo info = ((FieldItem) v.getItem()).getInfo();
			if (info == null) {
				logger.error("Failed to lookup field for tree cell '{}'", v.getPathElementValue());
				return null;
			}
			return getFieldIcon(info);
		});
		GRAPHIC_FUNCS.put(MethodItem.class, (w, v) -> {
			MethodInfo info = ((MethodItem) v.getItem()).getInfo();
			if (info == null) {
				logger.error("Failed to lookup method for tree cell '{}'", v.getPathElementValue());
				return null;
			}
			return getMethodIcon(info);
		});
		GRAPHIC_FUNCS.put(FileItem.class, (w, v) -> {
			String fileName = ((FileItem) v.getItem()).getFileName();
			FileInfo info = w.getResources().getFile(fileName);
			if (info == null) {
				logger.error("Failed to lookup file for tree cell '{}'", fileName);
				return getIconView(Icons.FILE_BINARY);
			}
			return getFileIcon(info);
		});
		// Context menus
		CONTEXT_FUNCS.put(ClassItem.class, (w, v) -> {
			Resources resources = w.getResources();
			ClassItem ci = (ClassItem) v.getItem();
			String name = ci.getClassName();
			ClassInfo info = resources.getClass(name);
			return ContextBuilder.forClass(info).withResource(ci.getContainingResource());
		});
		CONTEXT_FUNCS.put(DexClassItem.class, (w, v) -> {
			Resources resources = w.getResources();
			DexClassItem dci = (DexClassItem) v.getItem();
			String name = dci.getClassName();
			DexClassInfo info = resources.getDexClass(name);
			return ContextBuilder.forDexClass(info).withResource(dci.getContainingResource());
		});
		CONTEXT_FUNCS.put(PackageItem.class, (w, v) -> {
			PackageItem pi = (PackageItem) v.getItem();
			String name = pi.getFullPackageName();
			return ContextBuilder.forPackage(name).withResource(pi.getContainingResource());
		});
		CONTEXT_FUNCS.put(FieldInfo.class, (w, v) -> {
			Resources resources = w.getResources();
			ClassItem ci = (ClassItem) v.getItem().getParent();
			FieldItem fi = (FieldItem) v.getItem();
			String ownerName = ci.getClassName();
			ClassInfo info = resources.getClass(ownerName);
			return ContextBuilder.forField(info, fi.getInfo()).withResource(fi.getContainingResource());
		});
		CONTEXT_FUNCS.put(MethodItem.class, (w, v) -> {
			Resources resources = w.getResources();
			ClassItem ci = (ClassItem) v.getItem().getParent();
			MethodItem mi = (MethodItem) v.getItem();
			String ownerName = ci.getClassName();
			ClassInfo info = resources.getClass(ownerName);
			return ContextBuilder.forMethod(info, mi.getInfo()).withResource(mi.getContainingResource());
		});
		CONTEXT_FUNCS.put(FileItem.class, (w, v) -> {
			Resources resources = w.getResources();
			FileItem fi = (FileItem) v.getItem();
			String name = fi.getFileName();
			FileInfo info = resources.getFile(name);
			return ContextBuilder.forFile(info).withResource(fi.getContainingResource());
		});
		CONTEXT_FUNCS.put(DirectoryItem.class, (w, v) -> {
			DirectoryItem di = (DirectoryItem) v.getItem();
			String name = di.getFullDirectoryName();
			return ContextBuilder.forDirectory(name).withResource(di.getContainingResource());
		});
		CONTEXT_FUNCS.put(ResourceItem.class, (w, v) -> {
			ResourceItem ri = (ResourceItem) v.getItem();
			return ContextBuilder.forResource(ri.getResource()).withResource(ri.getResource());
		});
	}
}
