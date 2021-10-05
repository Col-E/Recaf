package me.coley.recaf.ui.control.tree.item;

import me.coley.recaf.code.FieldInfo;

/**
 * Item for representing fields in the workspace.
 *
 * @author Matt Coley
 */
public class FieldItem extends BaseTreeItem implements AnnotatableItem {
	private final FieldInfo info;
	private String annotation;

	/**
	 * @param info
	 * 		Field info of the item.
	 */
	public FieldItem(FieldInfo info) {
		this.info = info;
		init();
	}

	/**
	 * @return Field info.
	 */
	public FieldInfo getInfo() {
		return info;
	}

	@Override
	protected BaseTreeValue createTreeValue() {
		return new BaseTreeValue(this, info.getDescriptor() + " " + info.getName(), false) {
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
