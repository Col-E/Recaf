package me.coley.recaf.ui.control.tree.item;

import me.coley.recaf.RecafUI;
import me.coley.recaf.workspace.resource.Resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * Tree item for {@link Resource},
 *
 * @author Matt Coley
 */
public class ResourceItem extends BaseTreeItem {
	private final ResourceClassesItem classesItem = new ResourceClassesItem();
	private final ResourceFilesItem filesItem = new ResourceFilesItem();
	private final Resource resource;

	protected ResourceItem(Resource resource) {
		this.resource = resource;
		addChild(classesItem);
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
	}

	/**
	 * Add tree path.
	 *
	 * @param name
	 * 		Name of class.
	 */
	public void addClass(String name) {
		BaseTreeItem item = classesItem;
		List<String> parts = new ArrayList<>(Arrays.asList(name.split("/")));
		while(!parts.isEmpty()) {
			String part = parts.remove(0);
			boolean isLeaf = parts.isEmpty();
			BaseTreeItem child = isLeaf ?
					item.getChildFile(part) :
					item.getChildDirectory(part);
			if(child == null) {
				child = isLeaf ?
						new ClassItem(name) :
						new PackageItem(part);
				item.addChild(child);
			}
			item = child;
		}
	}

	/**
	 * Add tree path.
	 *
	 * @param name
	 * 		Name of file.
	 */
	public void addFile(String name) {
		BaseTreeItem item = filesItem;
		List<String> parts = new ArrayList<>(Arrays.asList(name.split("/")));
		while(!parts.isEmpty()) {
			String part = parts.remove(0);
			boolean isLeaf = parts.isEmpty();
			BaseTreeItem child = isLeaf ?
					item.getChildFile(part) :
					item.getChildDirectory(part);
			if(child == null) {
				child = isLeaf ?
						new FileItem(name) :
						new DirectoryItem(part);
				item.addChild(child);
			}
			item = child;
		}
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
	 * @param name
	 * 		Name of file.
	 */
	public void removeFile(String name) {
		remove(filesItem, name);
	}

	private void remove(BaseTreeItem root, String name) {
		BaseTreeItem item = root;
		BaseTreeItem parent = item;
		List<String> parts = new ArrayList<>(Arrays.asList(name.split("/")));
		while(!parts.isEmpty()) {
			String part = parts.remove(0);
			boolean isLeaf = parts.isEmpty();
			BaseTreeItem child = isLeaf ?
					item.getChildFile(part) :
					item.getChildDirectory(part);
			if(child == null) {
				return;
			}
			parent = item;
			item = child;
		}
		// Remove child from parent.
		// If parent is now empty, remove it as well.
		do {
			parent.removeChild(item);
			item = parent;
			parent = (BaseTreeItem) item.getParent();
		} while (parent.isLeaf());
	}

	@Override
	public int compareTo(BaseTreeItem other) {
		// Ensure primary workspace is first
		if (other instanceof ResourceItem) {
			Resource primary = RecafUI.getController().getWorkspace().getResources().getPrimary();
			if (primary == getResource()) {
				return -1;
			}
			String contentName = getResource().getContentSource().toString();
			String otherContentName = ((ResourceItem) other).getResource().toString();
			return contentName.compareTo(otherContentName);
		}
		return super.compareTo(other);
	}

	@Override
	protected BaseTreeValue createTreeValue() {
		return new BaseTreeValue(this, null, false);
	}
}
