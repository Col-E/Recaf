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
	 * @return Name of package.
	 */
	public String getPackageName() {
		return packageName;
	}

	@Override
	protected BaseTreeValue createTreeValue() {
		String partialName = packageName.substring(packageName.lastIndexOf('/') + 1);
		return new BaseTreeValue(this, partialName, true);
	}
}
