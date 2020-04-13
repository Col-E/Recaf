package me.coley.recaf.ui.controls.tree;

import me.coley.recaf.workspace.JavaResource;

/**
 * Tree item to represent packages.
 *
 * @author Matt
 */
public class PackageItem extends DirectoryItem {
	private final String packageName;

	/**
	 * @param resource
	 * 		The resource associated with the item.
	 * @param local
	 * 		Partial name of the package.
	 * @param packageName
	 * 		Full package name.
	 */
	public PackageItem(JavaResource resource, String local, String packageName) {
		super(resource, local);
		this.packageName = packageName;
	}

	/**
	 * @return Package name.
	 */
	public String getPackageName() {
		return packageName;
	}
}