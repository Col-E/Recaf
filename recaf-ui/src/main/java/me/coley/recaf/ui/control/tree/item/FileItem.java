package me.coley.recaf.ui.control.tree.item;

/**
 * Item for representing files in the workspace.
 *
 * @author Matt Coley
 */
public class FileItem extends BaseTreeItem {
	private final String fileName;

	/**
	 * @param fileName
	 * 		File name.
	 */
	public FileItem(String fileName) {
		this.fileName = fileName;
		init();
	}

	/**
	 * @return File name.
	 */
	public String getFileName() {
		return fileName;
	}

	@Override
	protected BaseTreeValue createTreeValue() {
		String simpleName = fileName.substring(fileName.lastIndexOf('/') + 1);
		return new BaseTreeValue(this, simpleName, false);
	}

	@Override
	public String toString() {
		return "File: " + fileName;
	}
}
