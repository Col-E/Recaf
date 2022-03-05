package me.coley.recaf.ui.control.tree.item;

/**
 * Item for representing packages in the workspace.
 *
 * @author Matt Coley
 */
public class PackageItem extends BaseTreeItem {
	private final String packageName;

	/**
	 * @param packageName
	 * 		Name of package.
	 */
	public PackageItem(String packageName) {
		this.packageName = packageName;
		init();
	}

	/**
	 * @return Local name of package.
	 */
	public String getLocalPackageName() {
		return packageName;
	}

	/**
	 * @return Full path name of package.
	 */
	public String getFullPackageName() {
		return getValue().getFullPath();
	}

	@Override
	protected BaseTreeValue createTreeValue() {
		String partialName = packageName.substring(packageName.lastIndexOf('/') + 1);
		return new BaseTreeValue(this, partialName, true) {
			@Override
			public ItemType getItemType() {
				return ItemType.PRIORITY_DIRECTORY;
			}
		};
	}

	@Override
	public String toString() {
		return "Package: " + packageName;
	}
}
