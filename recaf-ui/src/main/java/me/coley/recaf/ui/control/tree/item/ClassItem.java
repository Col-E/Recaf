package me.coley.recaf.ui.control.tree.item;

import java.util.Objects;

/**
 * Item for representing classes in the workspace.
 *
 * @author Matt Coley
 */
public class ClassItem extends BaseTreeItem implements AnnotatableItem {
	private final String className;
	private final boolean isDirectory;
	private String annotation;

	/**
	 * Class item as a leaf.
	 *
	 * @param className
	 * 		Class name.
	 */
	public ClassItem(String className) {
		this(className, false);
	}

	/**
	 * Class item as a branch.
	 *
	 * @param className
	 * 		Class name.
	 * @param isDirectory
	 * 		Is the item representing a directory.
	 * 		Should be {@code true} when {@link FieldItem} or {@link MethodItem} are to be added as children.
	 */
	public ClassItem(String className, boolean isDirectory) {
		this.className = Objects.requireNonNull(className, "Class name must not be null");
		this.isDirectory = isDirectory;
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
		return new BaseTreeValue(this, simpleName, isDirectory);
	}

	@Override
	public void setAnnotationType(String type) {
		this.annotation = type;
	}

	@Override
	public String getAnnotationType() {
		return annotation;
	}

	@Override
	public String toString() {
		return "Class: " + className;
	}
}
