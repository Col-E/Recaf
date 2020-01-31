package me.coley.recaf.ui.controls.tree;

import javafx.application.Platform;
import me.coley.recaf.workspace.JavaResource;

/**
 * Root item
 *
 * @author Matt
 */
public class RootItem extends BaseItem {
	private ClassFolderItem classes;
	private FileFolderItem files;

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
				Platform.runLater(() -> ((BaseItem) di.getParent()).getSourceChildren().remove(di));
			});
			resource.getClasses().getPutListeners().add((k, v) -> {
				// Put includes updates, so only "add" the class when it doesn't already exist
				if (!resource.getClasses().containsKey(k))
					Platform.runLater(() -> classes.addClass(k));
			});
		}
		// files sub-folder
		if(resource.getFiles().size() > 0) {
			getSourceChildren().add(files = new FileFolderItem(resource));
			// Register listeners and update if the files update
			resource.getFiles().getRemoveListeners().add(r -> {
				String name = r.toString();
				DirectoryItem di = files.getDeepChild(name);
				Platform.runLater(() -> ((BaseItem) di.getParent()).getSourceChildren().remove(di));
			});
			resource.getFiles().getPutListeners().add((k, v) -> {
				// Put includes updates, so only "add" the file when it doesn't already exist
				if (!resource.getFiles().containsKey(k))
					Platform.runLater(() -> files.addFile(k));
			});
		}
		// TODO: Sub-folders for these?
		//  - docs
		//  - sources
	}
}