package me.coley.recaf.ui.control.tree.item;

/**
 * Base {@link BaseTreeItem} value.
 *
 * @author Matt Coley
 */
public class BaseTreeValue {
	private final BaseTreeItem item;
	private final String pathElementValue;
	private final boolean isDirectory;
	private String fullPath;

	/**
	 * @param item
	 * 		Associated item.
	 * @param pathElementValue
	 * 		Current tree element path.
	 * @param isDirectory
	 * 		Flag for it the element represents a directory.
	 */
	public BaseTreeValue(BaseTreeItem item, String pathElementValue, boolean isDirectory) {
		this.item = item;
		this.pathElementValue = pathElementValue;
		this.isDirectory = isDirectory;
		validatePathElement(pathElementValue);
	}

	protected void validatePathElement(String pathElementValue) {
		if (pathElementValue != null && pathElementValue.indexOf('/') >= 0) {
			throw new IllegalStateException("Path element names must not have separator '/' character!");
		}
	}

	/**
	 * @return Associated item.
	 */
	public BaseTreeItem getItem() {
		return item;
	}

	/**
	 * @return Current tree element path.
	 */
	public String getPathElementValue() {
		return pathElementValue;
	}

	/**
	 * @return Full path of the element.
	 */
	public String getFullPath() {
		if (fullPath == null) {
			BaseTreeItem parentItem = (BaseTreeItem) getItem().getParent();
			if (parentItem == null || parentItem.getValue() == null || parentItem.getValue().getFullPath() == null) {
				fullPath = pathElementValue;
			} else {
				fullPath = parentItem.getValue().getFullPath() + "/" + pathElementValue;
			}
		}
		return fullPath;
	}

	/**
	 * @return Flag for it the element represents a directory.
	 */
	public ItemType getItemType() {
		return isDirectory ? ItemType.DIRECTORY : ItemType.FILE;
	}
}
