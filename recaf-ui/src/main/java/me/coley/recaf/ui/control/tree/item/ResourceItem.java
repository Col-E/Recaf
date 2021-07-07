package me.coley.recaf.ui.control.tree.item;

import me.coley.recaf.RecafUI;
import me.coley.recaf.config.Configs;
import me.coley.recaf.workspace.resource.Resource;

import java.util.*;
import java.util.function.Function;

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

	private void addPath(BaseTreeItem item, String name,
						 Function<String, BaseTreeItem> leafFunction,
						 Function<String, BaseTreeItem> branchFunction) {
		List<String> parts = new ArrayList<>(Arrays.asList(name.split("/")));
		// Prune tree directory middle section if it is obnoxiously long
		int maxDepth = Configs.display().maxTreeDirectoryDepth;
		if (maxDepth > 0 && parts.size() > maxDepth) {
			String lastPart = parts.get(parts.size() - 1);
			// We keep only elements between [0 ... maxDepth-1] and the last part
			parts = new ArrayList<>(parts.subList(0, maxDepth - 1));
			parts.add("...");
			parts.add(lastPart);
		}
		// Build directory structure
		int maxLen = Configs.display().maxTreeTextLength;
		while(!parts.isEmpty()) {
			String part = parts.remove(0);
			if (part.length() > maxLen)
				part = part.substring(0, maxLen) + "...";
			boolean isLeaf = parts.isEmpty();
			BaseTreeItem child = isLeaf ?
					item.getChildFile(part) :
					item.getChildDirectory(part);
			if(child == null) {
				child = isLeaf ?
						leafFunction.apply(name) :
						branchFunction.apply(part);
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
