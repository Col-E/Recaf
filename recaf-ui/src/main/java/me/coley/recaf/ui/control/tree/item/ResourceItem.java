package me.coley.recaf.ui.control.tree.item;

import me.coley.recaf.RecafUI;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.Resources;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * Tree item for {@link Resource},
 *
 * @author Matt Coley
 */
public class ResourceItem extends BaseTreeItem {
	private final Map<String, ResourceDexClassesItem> dexItems = new HashMap<>();
	private final ResourceClassesItem classesItem = new ResourceClassesItem();
	private final ResourceFilesItem filesItem = new ResourceFilesItem();
	private final Resource resource;

	protected ResourceItem(Resource resource) {
		this.resource = resource;
		if (resource.getDexClasses().isEmpty()) {
			// Add java classes
			addChild(classesItem);
		} else {
			// Add android classes
			for (String name : resource.getDexClasses().getBackingMap().keySet()) {
				ResourceDexClassesItem item = new ResourceDexClassesItem(name);
				dexItems.put(name, item);
				addChild(item);
			}
		}
		addChild(filesItem);
		init();
	}

	/**
	 * @return Associated resource.
	 */
	public Resource getResource() {
		return resource;
	}

	/**
	 * Adds child entries from the resource.
	 */
	public void addResourceChildren() {
		new TreeSet<>(resource.getClasses().keySet()).forEach(this::addClass);
		new TreeSet<>(resource.getFiles().keySet()).forEach(this::addFile);
		resource.getDexClasses().getBackingMap().forEach((dexName, map) ->
				new TreeSet<>(map.keySet())
						.forEach(className -> addDexClass(dexName, className)));
	}

	/**
	 * Add tree path.
	 *
	 * @param name
	 * 		Name of class.
	 */
	public void addClass(String name) {
		addPath(classesItem, name, ClassItem::new, PackageItem::new);
	}

	/**
	 * Add tree path.
	 *
	 * @param dexName
	 * 		Name of containing dex file.
	 * @param name
	 * 		Name of class.
	 */
	public void addDexClass(String dexName, String name) {
		ResourceDexClassesItem item = dexItems.get(dexName);
		if (item == null)
			throw new IllegalStateException("Invalid dex file name passed: " + dexName);
		addPath(item, name, DexClassItem::new, PackageItem::new);
	}

	/**
	 * Add tree path.
	 *
	 * @param name
	 * 		Name of file.
	 */
	public void addFile(String name) {
		addPath(filesItem, name, FileItem::new, DirectoryItem::new);
	}

	/**
	 * Remove tree path.
	 *
	 * @param name
	 * 		Name of class.
	 */
	public void removeClass(String name) {
		remove(classesItem, name);
	}

	/**
	 * Remove tree path.
	 *
	 * @param dexName
	 * 		Name of containing dex file.
	 * @param name
	 * 		Name of class.
	 */
	public void removeDexClass(String dexName, String name) {
		ResourceDexClassesItem item = dexItems.get(dexName);
		if (item == null)
			throw new IllegalStateException("Invalid dex file name passed: " + dexName);
		remove(item, name);
	}

	/**
	 * Remove tree path.
	 *
	 * @param name
	 * 		Name of file.
	 */
	public void removeFile(String name) {
		remove(filesItem, name);
	}

	@Override
	public int compareTo(BaseTreeItem other) {
		if (other instanceof ResourceItem) {
			// Ensure primary workspace is first.
			Resources resources = RecafUI.getController().getWorkspace().getResources();
			Resource primary = resources.getPrimary();
			if (primary == getResource()) {
				return -1;
			}
			// Use library order in list.
			int myIndex = resources.getLibraries().indexOf(getResource());
			int otherIndex = resources.getLibraries().indexOf(((ResourceItem) other).getResource());
			return Integer.compare(myIndex, otherIndex);
		}
		return super.compareTo(other);
	}

	@Override
	protected BaseTreeValue createTreeValue() {
		return new BaseTreeValue(this, null, false);
	}

	@Override
	public boolean forceVisible() {
		return true;
	}
}
