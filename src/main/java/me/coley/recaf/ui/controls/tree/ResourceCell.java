package me.coley.recaf.ui.controls.tree;

import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import me.coley.recaf.ui.controls.IconView;

/**
 * Cell renderer.
 *
 * @author Matt
 */
public class ResourceCell extends TreeCell {
	@Override
	@SuppressWarnings("unchecked")
	public void updateItem(Object item, boolean empty) {
		super.updateItem(item, empty);
		if(!empty) {
			Class<?> k = getTreeItem().getClass();
			Node g = null;
			String t = null;
			// Draw root
			if(k.equals(RootItem.class)) {
				g = new IconView("icons/jar.png");
				t = getTreeItem().getValue().toString();
			}
			// Draw root:classes
			else if(k.equals(ClassFolderItem.class)) {
				g = new IconView("icons/folder-source.png");
				BaseItem b = (BaseItem) getTreeItem();
				int count = b.resource().getClasses().size();
				t = String.format("classes (%d)", count);
			}
			// Draw root:resources
			else if(k.equals(ResourceFolderItem.class)) {
				g = new IconView("icons/folder-resource.png");
				BaseItem b = (BaseItem) getTreeItem();
				int count = b.resource().getResources().size();
				t = String.format("resources (%d)", count);
			}
			// Draw classes
			else if(k.equals(ClassItem.class)) {
				ClassItem ci = (ClassItem) getTreeItem();
				g = ci.createGraphic();
				t = ci.getLocalName();
			}
			// Draw resources
			else if(k.equals(ResourceItem.class)) {
				ResourceItem ci = (ResourceItem) getTreeItem();
				g = ci.createGraphic();
				t = ci.getLocalName();
			}
			// Draw directory/folders
			else if(k.equals(DirectoryItem.class)) {
				DirectoryItem di = (DirectoryItem) getTreeItem();
				g = new IconView("icons/class/package-flat.png");
				t = di.getLocalName();
			}
			setGraphic(g);
			setText(t);
		} else {
			setGraphic(null);
			setText(null);
		}
	}
}