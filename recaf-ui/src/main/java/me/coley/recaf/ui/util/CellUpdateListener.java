package me.coley.recaf.ui.util;

import javafx.scene.control.Cell;
import me.coley.recaf.code.*;
import me.coley.recaf.ui.control.tree.CellOriginType;
import me.coley.recaf.ui.control.tree.item.*;
import me.coley.recaf.workspace.resource.Resource;

/**
 * Allow scripts and plugins to register callbacks on cell updates.
 *
 * @author xtherk
 */
public interface CellUpdateListener {
	/**
	 * Intercept context-menus for dex-classes.
	 */
	default void forDexClass(CellOriginType treeType, Cell<?> cell, Resource resource, DexClassInfo info) {
	}

	/**
	 * Intercept context-menus for classes.
	 */
	default void forClass(CellOriginType treeType, Cell<?> cell, Resource resource, ClassInfo info) {
	}

	/**
	 * Intercept context-menus for methods.
	 */
	default void forMethod(CellOriginType treeType, Cell<?> cell, Resource resource, MethodInfo info) {
	}

	/**
	 * Intercept context-menus for fields.
	 */
	default void forField(CellOriginType treeType, Cell<?> cell, Resource resource, FieldInfo info) {
	}

	/**
	 * Intercept context-menus for files.
	 */
	default void forFile(CellOriginType treeType, Cell<?> cell, Resource resource, FileInfo info) {
	}

	/**
	 * Intercept context-menus for packages.
	 */
	default void forPackage(CellOriginType treeType, Cell<?> cell, Resource resource, PackageItem item) {
	}

	/**
	 * Intercept context-menus for directories.
	 */
	default void forDirectory(CellOriginType treeType, Cell<?> cell, Resource resource, DirectoryItem item) {
	}

	/**
	 * Intercept context-menus for resources.
	 */
	default void forResource(CellOriginType treeType, Cell<?> cell, Resource resource, ResourceItem item) {
	}

	/**
	 * Intercept context-menus for resource-classes.
	 */
	default void forResourceClasses(CellOriginType treeType, Cell<?> cell, Resource resource, ResourceClassesItem item) {
	}

	/**
	 * Intercept context-menus for resource-files.
	 */
	default void forResourceFiles(CellOriginType treeType, Cell<?> cell, Resource resource, ResourceFilesItem item) {
	}

	/**
	 * Intercept context-menus for resource-dex-classes.
	 */
	default void forResourceDexClasses(CellOriginType treeType, Cell<?> cell, Resource resource, ResourceDexClassesItem item) {
	}

	/**
	 * Intercept context-menus for instructions.
	 */
	default void forInsn(CellOriginType treeType, Cell<?> cell, Resource resource, InsnItem item) {
	}
}
