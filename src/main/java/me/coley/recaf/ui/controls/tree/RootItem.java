package me.coley.recaf.ui.controls.tree;

import me.coley.recaf.workspace.JavaResource;

/**
 * Root item
 *
 * @author Matt
 */
public class RootItem extends BaseItem {
	private ClassFolderItem classes;
	private ResourceFolderItem resources;

	/**
	 * @param resource
	 * 		The resource associated with the item.
	 */
	public RootItem(JavaResource resource) {
		super(resource);
		// classes sub-folder
		if(resource.getClasses().size() > 0) {
			getChildren().add(classes = new ClassFolderItem(resource));
			// Register listeners and update if the classes update
			resource.getClasses().getRemoveListeners().add(r -> {
				String name = r.toString();
				DirectoryItem di = classes.getDeepChild(name);
				di.getParent().getChildren().remove(di);
			});
			resource.getClasses().getPutListeners().add((k, v) -> {
				classes.addClass(k);
			});
		}
		// resources sub-folder
		if(resource.getResources().size() > 0) {
			getChildren().add(resources = new ResourceFolderItem(resource));
			// Register listeners and update if the resources update
			resource.getResources().getRemoveListeners().add(r -> {
				String name = r.toString();
				DirectoryItem di = resources.getDeepChild(name);
				di.getParent().getChildren().remove(di);
			});
			resource.getResources().getPutListeners().add((k, v) -> {
				resources.addResource(k);
			});
		}
		// TODO: Sub-folders for these?
		//  - docs
		//  - sources
	}
}