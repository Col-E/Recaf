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
			getSourceChildren().add(classes = new ClassFolderItem(resource));
			// Register listeners and update if the classes update
			resource.getClasses().getRemoveListeners().add(r -> {
				String name = r.toString();
				DirectoryItem di = classes.getDeepChild(name);
				((BaseItem) di.getParent()).getSourceChildren().remove(di);
			});
			resource.getClasses().getPutListeners().add((k, v) -> {
				// Put includes updates, so only "add" the class when it doesn't already exist
				if (!resource.getClasses().containsKey(k))
					classes.addClass(k);
			});
		}
		// resources sub-folder
		if(resource.getResources().size() > 0) {
			getSourceChildren().add(resources = new ResourceFolderItem(resource));
			// Register listeners and update if the resources update
			resource.getResources().getRemoveListeners().add(r -> {
				String name = r.toString();
				DirectoryItem di = resources.getDeepChild(name);
				((BaseItem) di.getParent()).getSourceChildren().remove(di);
			});
			resource.getResources().getPutListeners().add((k, v) -> {
				// Put includes updates, so only "add" the resource when it doesn't already exist
				if (!resource.getResources().containsKey(k))
					resources.addResource(k);
			});
		}
		// TODO: Sub-folders for these?
		//  - docs
		//  - sources
	}
}