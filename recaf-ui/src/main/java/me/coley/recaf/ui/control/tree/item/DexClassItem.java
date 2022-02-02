package me.coley.recaf.ui.control.tree.item;

import javafx.scene.control.TreeItem;

import java.util.Objects;

/**
 * Item for representing classes in the workspace.
 *
 * @author Matt Coley
 */
public class DexClassItem extends BaseTreeItem {
	private final String className;

	/**
	 * @param className
	 * 		Class name.
	 */
	public DexClassItem(String className) {
		this.className = Objects.requireNonNull(className, "Class name must not be null");
		init();
	}

	/**
	 * @return Name of dex file class is defined in.
	 */
	public String getAssociatedDexFile() {
		TreeItem<?> item = this;
		while (!(item instanceof ResourceDexClassesItem)) {
			item = item.getParent();
		}
		return ((ResourceDexClassesItem) item).getDexName();
	}

	/**
	 * @return Class name.
	 */
	public String getClassName() {
		return className;
	}

	@Override
	protected BaseTreeValue createTreeValue() {
		String simpleName = className.substring(className.lastIndexOf('/') + 1);
		return new BaseTreeValue(this, simpleName, false);
	}

	@Override
	public String toString() {
		return "DexClass: " + className;
	}
}
