package me.coley.recaf.ui.util;

import javafx.scene.control.Cell;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.*;
import me.coley.recaf.ui.context.ContextBuilder;
import me.coley.recaf.ui.context.ContextSource;
import me.coley.recaf.ui.control.tree.CellOriginType;
import me.coley.recaf.ui.control.tree.item.*;
import me.coley.recaf.util.TextDisplayUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.Resources;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	private static final List<CellUpdateListener> LISTENERS = new ArrayList<>();

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
			cell.setText(TextDisplayUtil.shortenEscapeLimit(className));
			IconProvider iconProvider = Icons.getClassIconProvider(classInfo);
			cell.setContextMenu(ContextBuilder.forClass(classInfo)
				.setIcon(iconProvider.makeIcon())
				.withResource(resource)
				.setWhere(from(type))
				.build());
			cell.setGraphic(iconProvider.makeIcon());
			LISTENERS.forEach(it -> it.forClass(type, cell, resource, classInfo));
		});
		INFO_MAP.put(DexClassInfo.class, (type, cell, resource, info) -> {
			DexClassInfo classInfo = (DexClassInfo) info;
			String className = info.getName();
			cell.setText(TextDisplayUtil.shortenEscapeLimit(className));
			IconProvider iconProvider = Icons.getClassIconProvider(classInfo);
			cell.setContextMenu(ContextBuilder.forDexClass(classInfo)
				.setIcon(iconProvider.makeIcon())
				.withResource(resource)
				.setWhere(from(type))
				.build());
			cell.setGraphic(iconProvider.makeIcon());
			LISTENERS.forEach(it -> it.forDexClass(type, cell, resource, classInfo));
		});
		INFO_MAP.put(FileInfo.class, (type, cell, resource, info) -> {
			FileInfo fileInfo = (FileInfo) info;
			String fileName = info.getName();
			cell.setText(TextDisplayUtil.shortenEscapeLimit(fileName));
			IconProvider iconProvider = getFileIconProvider(fileInfo);
			cell.setContextMenu(ContextBuilder.forFile(fileInfo)
				.setIcon(iconProvider.makeIcon())
				.withResource(resource)
				.setWhere(from(type))
				.build());

			cell.setGraphic(iconProvider.makeIcon());
			LISTENERS.forEach(it -> it.forFile(type, cell, resource, fileInfo));
		});
		INFO_MAP.put(FieldInfo.class, (type, cell, resource, info) -> {
			Resources resources = RecafUI.getController().getWorkspace().getResources();
			FieldInfo fieldInfo = (FieldInfo) info;
			CommonClassInfo owner = resources.getClass(fieldInfo.getOwner());
			if (owner == null) {
				owner = resources.getClass(fieldInfo.getOwner());
			}
			String name = info.getName();
			cell.setText(TextDisplayUtil.shortenEscapeLimit(name));
			cell.setGraphic(getFieldIcon(fieldInfo));
			cell.setContextMenu(ContextBuilder.forField(owner, fieldInfo)
					.withResource(resource)
					.setWhere(from(type))
					.build());
			LISTENERS.forEach(it -> it.forField(type, cell, resource, fieldInfo));
		});
		INFO_MAP.put(MethodInfo.class, (type, cell, resource, info) -> {
			Resources resources = RecafUI.getController().getWorkspace().getResources();
			MethodInfo methodInfo = (MethodInfo) info;
			CommonClassInfo owner = resources.getClass(methodInfo.getOwner());
			if (owner == null) {
				owner = resources.getClass(methodInfo.getOwner());
			}
			String name = info.getName();
			cell.setText(TextDisplayUtil.shortenEscapeLimit(name));
			cell.setGraphic(getMethodIcon(methodInfo));
			cell.setContextMenu(ContextBuilder.forMethod(owner, methodInfo)
					.withResource(resource)
					.setWhere(from(type))
					.build());
			LISTENERS.forEach(it -> it.forMethod(type, cell, resource, methodInfo));
		});
		//
		ITEM_MAP.put(WorkspaceRootItem.class, (type, cell, resource, item) -> {
			cell.setText("Root");
		});
		ITEM_MAP.put(ResultsRootItem.class, (type, cell, resource, item) -> {
			ResultsRootItem i = (ResultsRootItem) item;
			cell.setGraphic(null);
			cell.setText(String.format("%s - %d results", i.getSearch().toString(), i.getResults().size()));
		});
		ITEM_MAP.put(DummyItem.class, (type, cell, resource, item) -> {
			DummyItem i = (DummyItem) item;
			cell.textProperty().bind(i.getDummyText());
			cell.setGraphic(null);
		});
		ITEM_MAP.put(ResourceItem.class, (type, cell, resource, item) -> {
			ResourceItem i = (ResourceItem) item;
			cell.setText(i.getResource().getContentSource().toString());
			cell.setGraphic(Icons.getResourceIcon(i.getResource()));
			cell.setContextMenu(ContextBuilder.forResource(resource)
					.withResource(resource)
					.setWhere(from(type))
					.build());
			LISTENERS.forEach(it -> it.forResource(type, cell, resource, i));
		});
		ITEM_MAP.put(ResourceClassesItem.class, (type, cell, resource, item) -> {
			cell.textProperty().bind(Lang.getBinding("tree.classes"));
			cell.setGraphic(getIconView(Icons.FOLDER_SRC));
			LISTENERS.forEach(it -> it.forResourceClasses(type, cell, resource, (ResourceClassesItem) item));
		});
		ITEM_MAP.put(ResourceFilesItem.class, (type, cell, resource, item) -> {
			cell.textProperty().bind(Lang.getBinding("tree.files"));
			cell.setGraphic(getIconView(Icons.FOLDER_RES));
			LISTENERS.forEach(it -> it.forResourceFiles(type, cell, resource, (ResourceFilesItem) item));
		});
		ITEM_MAP.put(ResourceDexClassesItem.class, (type, cell, resource, item) -> {
			ResourceDexClassesItem i = (ResourceDexClassesItem) item;
			cell.setText(i.getDexName());
			cell.setGraphic(getIconView(Icons.FOLDER_SRC));
			LISTENERS.forEach(it -> it.forResourceDexClasses(type, cell, resource, i));
		});
		ITEM_MAP.put(PackageItem.class, (type, cell, resource, item) -> {
			PackageItem i = (PackageItem) item;
			cell.setGraphic(getIconView(Icons.FOLDER_PACKAGE));
			String name = i.getFullPackageName();
			cell.setContextMenu(ContextBuilder.forPackage(name)
					.withResource(resource)
					.setWhere(from(type))
					.build());
			LISTENERS.forEach(it -> it.forPackage(type, cell, resource, i));
		});
		ITEM_MAP.put(DirectoryItem.class, (type, cell, resource, item) -> {
			DirectoryItem i = (DirectoryItem) item;
			cell.setGraphic(getIconView(Icons.FOLDER));
			String name = i.getFullDirectoryName();
			cell.setContextMenu(ContextBuilder.forDirectory(name)
					.withResource(resource)
					.setWhere(from(type))
					.build());
			LISTENERS.forEach(it -> it.forDirectory(type, cell, resource, i));
		});
		ITEM_MAP.put(InsnItem.class, (type, cell, resource, item) -> {
			Resources resources = RecafUI.getController().getWorkspace().getResources();
			MethodItem i = (MethodItem) ((InsnItem) item).getParent();
			MethodInfo methodInfo = i.getInfo();
			CommonClassInfo owner = resources.getClass(methodInfo.getOwner());
			cell.setGraphic(getIconView(CODE));
			cell.setContextMenu(ContextBuilder.forMethodInstruction(owner, methodInfo)
					.withResource(resource)
					.setWhere(from(type))
					.build());
			LISTENERS.forEach(it -> it.forInsn(type, cell, resource, (InsnItem) item));
		});
	}


	/**
	 * @param listener
	 * 		Context menu event listener to add.
	 */
	public static void addListener(CellUpdateListener listener) {
		LISTENERS.add(listener);
	}

	/**
	 * @param listener
	 * 		Context menu event listener to remove.
	 */
	public static void removeListener(CellUpdateListener listener) {
		LISTENERS.remove(listener);
	}
}
