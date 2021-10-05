package me.coley.recaf.ui.control.tree.item;

import me.coley.recaf.code.MethodInfo;

/**
 * Item for representing methods in the workspace.
 *
 * @author Matt Coley
 */
public class MethodItem extends BaseTreeItem implements AnnotatableItem {
	private final MethodInfo info;
	private final boolean isDirectory;
	private String annotation;

	/**
	 * @param info
	 * 		Method info of the item.
	 * @param isDirectory
	 * 		Is the item representing a directory.
	 * 		Should be {@code true} when an {@link InsnItem} is to be added as children.
	 */
	public MethodItem(MethodInfo info, boolean isDirectory) {
		this.info = info;
		this.isDirectory = isDirectory;
		init();
	}

	/**
	 * @return Method info.
	 */
	public MethodInfo getInfo() {
		return info;
	}

	@Override
	protected BaseTreeValue createTreeValue() {
		return new BaseTreeValue(this, info.getName() + info.getDescriptor(), isDirectory) {
			@Override
			protected void validatePathElement(String pathElementValue) {
				// no-op
			}
		};
	}

	@Override
	public void setAnnotationType(String type) {
		this.annotation = type;
	}

	@Override
	public String getAnnotationType() {
		return annotation;
	}
}
