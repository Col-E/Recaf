package me.coley.recaf.ui.control.tree.item;

import java.util.Objects;

/**
 * Item for representing classes in the workspace.
 *
 * @author Matt Coley
 */
public class ClassItem extends BaseTreeItem {
	private final String className;

	/**
	 * @param className
	 * 		Class name.
	 */
	public ClassItem(String className) {
		this.className = Objects.requireNonNull(className, "Class name must not be null");
		init();
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
}
