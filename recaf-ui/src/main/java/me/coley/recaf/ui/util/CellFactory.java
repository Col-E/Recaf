package me.coley.recaf.ui.util;

import javafx.scene.control.Cell;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.*;
import me.coley.recaf.ui.context.ContextBuilder;
import me.coley.recaf.ui.context.ContextSource;
import me.coley.recaf.ui.control.tree.CellOriginType;
import me.coley.recaf.ui.control.tree.item.*;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.Resources;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static me.coley.recaf.ui.util.Icons.*;

/**
 * Utility to populate cell properties based on their content.
 * Any cell type <i>(list, table, etc)</i> only needs to call {@link #update(CellOriginType, Cell, Resource, Object)}.
 *
 * @author Matt Coley
 */
public class CellFactory {
	private static final Logger logger = Logging.get(CellFactory.class);
	private static final Map<Class<?>, CellUpdater<ItemInfo>> INFO_MAP = new HashMap<>();
	private static final Map<Class<?>, CellUpdater<Object>> ITEM_MAP = new HashMap<>();

	/**
	 * @param type
	 * 		Cell location in the UI. Affects context menu availability.
	 * @param cell
	 * 		The cell to update.
	 * @param resource
	 * 		The resource the value is associated with.
	 * @param value
	 * 		The value. Should be a type of {@link ItemInfo} or cell {@link Class} .
	 */
	public static void update(CellOriginType type, Cell<?> cell, Resource resource, Object value) {
		if (value instanceof ItemInfo) {
			// Update based on info type
			CellUpdater<ItemInfo> updater = INFO_MAP.get(value.getClass());
			if (updater != null)
				updater.update(type, cell, resource, (ItemInfo) value);
			else
				logger.warn("No updater for info: " + value.getClass().getName());
		} else if (value != null) {
			// Update based on cell type
			CellUpdater<Object> updater = ITEM_MAP.get(value.getClass());
			if (updater != null)
				updater.update(type, cell, resource, value);
			else
				logger.warn("No updater for cell: " + value.getClass().getName());
		}
	}

	/**
	 * @param type
	 * 		Cell location in the UI.
	 *
	 * @return Context menu location in the UI.
	 */
	private static ContextSource from(CellOriginType type) {
		switch (type) {
			default:
			case WORKSPACE_NAVIGATION:
				return ContextSource.WORKSPACE_TREE;
			case QUICK_NAV:
			case SEARCH_RESULTS:
				return ContextSource.SEARCH_RESULTS;
		}
	}

	private interface CellUpdater<T> {
		void update(CellOriginType treeType, Cell<?> cell, Resource resource, T value);
	}

	static {
		INFO_MAP.put(ClassInfo.class, (type, cell, resource, info) -> {
			ClassInfo classInfo = (ClassInfo) info;
			String className = info.getName();
			cell.setText(StringUtil.shortenPath(className));
			cell.setGraphic(getClassIcon(classInfo));
			cell.setContextMenu(ContextBuilder.forClass(classInfo)
					.withResource(resource)
					.setWhere(from(type))
					.build());
		});
		INFO_MAP.put(DexClassInfo.class, (type, cell, resource, info) -> {
			DexClassInfo classInfo = (DexClassInfo) info;
			String className = info.getName();
			cell.setText(StringUtil.shortenPath(className));
			cell.setGraphic(getClassIcon(classInfo));
			cell.setContextMenu(ContextBuilder.forDexClass(classInfo)
					.withResource(resource)
					.setWhere(from(type))
					.build());
		});
		INFO_MAP.put(FileInfo.class, (type, cell, resource, info) -> {
			FileInfo fileInfo = (FileInfo) info;
			String fileName = info.getName();
			cell.setText(StringUtil.shortenPath(fileName));
			cell.setGraphic(getFileIcon(fileInfo));
			cell.setContextMenu(ContextBuilder.forFile(fileInfo)
					.withResource(resource)
					.setWhere(from(type))
					.build());
		});
		INFO_MAP.put(FieldInfo.class, (type, cell, resource, info) -> {
			Resources resources = RecafUI.getController().getWorkspace().getResources();
			FieldInfo fieldInfo = (FieldInfo) info;
			CommonClassInfo owner = resources.getClass(fieldInfo.getOwner());
			if (owner == null) {
				owner = resources.getClass(fieldInfo.getOwner());
			}
			String name = info.getName();
			cell.setText(name);
			cell.setGraphic(getFieldIcon(fieldInfo));
			cell.setContextMenu(ContextBuilder.forField(owner, fieldInfo)
					.withResource(resource)
					.setWhere(from(type))
					.build());
		});
		INFO_MAP.put(MethodInfo.class, (type, cell, resource, info) -> {
			Resources resources = RecafUI.getController().getWorkspace().getResources();
			MethodInfo methodInfo = (MethodInfo) info;
			CommonClassInfo owner = resources.getClass(methodInfo.getOwner());
			if (owner == null) {
				owner = resources.getClass(methodInfo.getOwner());
			}
			String name = info.getName();
			cell.setText(name);
			cell.setGraphic(getMethodIcon(methodInfo));
			cell.setContextMenu(ContextBuilder.forMethod(owner, methodInfo)
					.withResource(resource)
					.setWhere(from(type))
					.build());
		});
		//
		ITEM_MAP.put(WorkspaceRootItem.class, (type, cell, resource, item) -> {
			cell.setText("Root");
		});
		ITEM_MAP.put(ResultsRootItem.class, (type, cell, resource, item) -> {
			ResultsRootItem i = (ResultsRootItem) item;
			cell.setText(String.format("%s - %d results", i.getSearch().toString(), i.getResults().size()));
		});
		ITEM_MAP.put(DummyItem.class, (type, cell, resource, item) -> {
			DummyItem i = (DummyItem) item;
			cell.textProperty().bind(i.getDummyText());
		});
		ITEM_MAP.put(ResourceItem.class, (type, cell, resource, item) -> {
			ResourceItem i = (ResourceItem) item;
			cell.setText(i.getResource().getContentSource().toString());
			cell.setGraphic(Icons.getResourceIcon(i.getResource()));
			cell.setContextMenu(ContextBuilder.forResource(resource)
					.withResource(resource)
					.setWhere(from(type))
					.build());
		});
		ITEM_MAP.put(ResourceClassesItem.class, (type, cell, resource, item) -> {
			cell.textProperty().bind(Lang.getBinding("tree.classes"));
			cell.setGraphic(getIconView(Icons.FOLDER_SRC));
		});
		ITEM_MAP.put(ResourceFilesItem.class, (type, cell, resource, item) -> {
			cell.textProperty().bind(Lang.getBinding("tree.files"));
			cell.setGraphic(getIconView(Icons.FOLDER_RES));
		});
		ITEM_MAP.put(ResourceDexClassesItem.class, (type, cell, resource, item) -> {
			ResourceDexClassesItem i = (ResourceDexClassesItem) item;
			cell.setText(i.getDexName());
			cell.setGraphic(getIconView(Icons.FOLDER_SRC));
		});
		ITEM_MAP.put(PackageItem.class, (type, cell, resource, item) -> {
			PackageItem i = (PackageItem) item;
			cell.setGraphic(getIconView(Icons.FOLDER_PACKAGE));
			String name = i.getFullPackageName();
			cell.setContextMenu(ContextBuilder.forPackage(name)
					.withResource(resource)
					.setWhere(from(type))
					.build());
		});
		ITEM_MAP.put(DirectoryItem.class, (type, cell, resource, item) -> {
			DirectoryItem i = (DirectoryItem) item;
			cell.setGraphic(getIconView(Icons.FOLDER));
			String name = i.getFullDirectoryName();
			cell.setContextMenu(ContextBuilder.forDirectory(name)
					.withResource(resource)
					.setWhere(from(type))
					.build());
		});
		ITEM_MAP.put(InsnItem.class, (type, cell, resource, item) -> {
			cell.setGraphic(getIconView(CODE));
		});
	}
}
