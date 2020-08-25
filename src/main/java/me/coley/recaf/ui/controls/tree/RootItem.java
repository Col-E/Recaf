package me.coley.recaf.ui.controls.tree;

import javafx.application.Platform;
import me.coley.recaf.util.struct.InternalBiConsumer;
import me.coley.recaf.util.struct.InternalConsumer;
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
			addSourceChild(classes = new ClassFolderItem(resource));
			// Register listeners and update if the classes update
			resource.getClasses().getRemoveListeners().add(InternalConsumer.internal(r -> {
				String name = r.toString();
				DirectoryItem di = classes.getDeepChild(name);
				if (di != null) {
					Platform.runLater(() -> {
						BaseItem parent = (BaseItem) di.getParent();
						if(parent != null) {
							parent.removeSourceChild(di);
							// Remove directories if needed
							while(parent.isLeaf() && !(parent instanceof ClassFolderItem)) {
								BaseItem parentOfParent = (BaseItem) parent.getParent();
								parentOfParent.removeSourceChild(parent);
								parent = parentOfParent;
							}
						}
					});
				}
			}));
			resource.getClasses().getPutListeners().add(InternalBiConsumer.internal((k, v) -> {
				// Put includes updates, so only "add" the class when it doesn't already exist
				if (!resource.getClasses().containsKey(k))
					Platform.runLater(() -> classes.addClass(k));
			}));
		}
		// files sub-folder
		if(resource.getFiles().size() > 0) {
			addSourceChild(files = new FileFolderItem(resource));
			// Register listeners and update if the files update
			resource.getFiles().getRemoveListeners().add(InternalConsumer.internal(r -> {
				String name = r.toString();
				DirectoryItem di = files.getDeepChild(name);
				if (di != null) {
					Platform.runLater(() -> {
						BaseItem parent = (BaseItem) di.getParent();
						if(parent != null) {
							parent.removeSourceChild(di);
							// Remove directories if needed
							while(parent.isLeaf() && !(parent instanceof FileFolderItem)) {
								BaseItem parentOfParent = (BaseItem) parent.getParent();
								parentOfParent.removeSourceChild(parent);
								parent = parentOfParent;
							}
						}
					});
				}
			}));
			resource.getFiles().getPutListeners().add(InternalBiConsumer.internal((k, v) -> {
				// Put includes updates, so only "add" the file when it doesn't already exist
				if (!resource.getFiles().containsKey(k))
					Platform.runLater(() -> files.addFile(k));
			}));
		}
		// TODO: Sub-folders for these?
		//  - docs
		//  - sources
	}
}