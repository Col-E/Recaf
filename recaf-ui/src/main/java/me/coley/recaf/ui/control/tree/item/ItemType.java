package me.coley.recaf.ui.control.tree.item;

/**
 * Enum for describing the file/directory <i>(leaf/branch)</i> type of the {@link BaseTreeValue}.
 * <br>
 * Allows for multiple sub-types of the <i>file vs directory</i> setup.
 * Some directories may need to be pinned to the top for example, and this makes that relatively easy.
 * For an example see {@link PackageItem#createTreeValue()}.
 *
 * @author Matt Coley
 */
public enum ItemType {
	FILE,
	PRIORITY_FILE,
	DIRECTORY,
	PRIORITY_DIRECTORY;

	/**
	 * @return If the type indicates a directory.
	 */
	public boolean isDirectory() {
		return ordinal() >= DIRECTORY.ordinal();
	}
}
