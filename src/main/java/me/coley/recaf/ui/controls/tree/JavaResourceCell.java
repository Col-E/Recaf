package me.coley.recaf.ui.controls.tree;

import com.google.common.base.Joiner;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import me.coley.recaf.ui.controls.IconView;
import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.util.UiUtil;
import me.coley.recaf.workspace.JavaResource;

/**
 * Cell renderer.
 *
 * @author Matt
 */
public class JavaResourceCell extends TreeCell {
	@Override
	@SuppressWarnings("unchecked")
	public void updateItem(Object item, boolean empty) {
		super.updateItem(item, empty);
		if (empty) {
			setGraphic(null);
			setText(null);
		} else {
			Class<?> k = getTreeItem().getClass();
			Node g = null;
			String t = null;
			// Draw root
			if(k.equals(RootItem.class)) {
				t = getTreeItem().getValue().toString();
				g = new IconView(UiUtil.getResourceIcon((JavaResource) getTreeItem().getValue()));
				getStyleClass().add("tree-cell-root");
			} else if(k.equals(SearchRootItem.class)) {
				SearchRootItem sri = (SearchRootItem) getTreeItem();
				t = "Search[" + Joiner.on(", ").withKeyValueSeparator("=").join(sri.getParams())  + "] - " +
						sri.getResults().size() + " results";
				g = new IconView(UiUtil.getResourceIcon((JavaResource) getTreeItem().getValue()));
				getStyleClass().add("tree-cell-root");
			}
			// Draw root:classes
			else if(k.equals(ClassFolderItem.class)) {
				g = new IconView("icons/folder-source.png");
				BaseItem b = (BaseItem) getTreeItem();
				int count = b.resource().getClasses().size();
				t = String.format("classes (%d)", count);
				getStyleClass().add("tree-cell-root-classes");
			}
			// Draw root:files
			else if(k.equals(FileFolderItem.class)) {
				g = new IconView("icons/folder-resource.png");
				BaseItem b = (BaseItem) getTreeItem();
				int count = b.resource().getFiles().size();
				t = String.format("files (%d)", count);
				getStyleClass().add("tree-cell-root-files");
			}
			// Draw classes
			else if(k.equals(ClassItem.class)) {
				ClassItem ci = (ClassItem) getTreeItem();
				int access = ClassUtil.getAccess(ci.resource().getClasses().get(ci.getClassName()));
				g = UiUtil.createClassGraphic(access);
				t = ci.getLocalName();
				getStyleClass().add("tree-cell-class");
			}
			// Draw files
			else if(k.equals(FileItem.class)) {
				FileItem fi = (FileItem) getTreeItem();
				g = UiUtil.createFileGraphic(fi.getLocalName());
				t = fi.getLocalName();
				getStyleClass().add("tree-cell-file");
			}
			// Draw members
			else if(k.equals(MemberItem.class)) {
				MemberItem mi = (MemberItem) getTreeItem();
				if (mi.isField())
					g = UiUtil.createFieldGraphic(mi.getMemberAccess());
				else
					g = UiUtil.createMethodGraphic(mi.getMemberAccess());
				t = mi.getLocalName();
				getStyleClass().add("tree-cell-member");
			}
			// Draw annotations
			else if(k.equals(AnnoItem.class)) {
				AnnoItem ai = (AnnoItem) getTreeItem();
				g = new IconView("icons/class/annotation.png");
				t = ai.getLocalName();
				getStyleClass().add("tree-cell-annotation");
			}
			// Draw instructions
			else if(k.equals(InsnItem.class)) {
				InsnItem ii = (InsnItem) getTreeItem();
				// TODO: Graphical representation instead to allow syntax highlighting
				g = new IconView("icons/result.png");
				t = ii.getLocalName();
				getStyleClass().add("tree-cell-instruction");
			}
			// Draw directory/folders
			else if(k.equals(DirectoryItem.class)) {
				DirectoryItem di = (DirectoryItem) getTreeItem();
				g = new IconView("icons/class/package-flat.png");
				t = di.getLocalName();
				getStyleClass().add("tree-cell-directory");
			}
			// Draw misc
			else if(k.equals(MiscItem.class)) {
				DirectoryItem di = (DirectoryItem) getTreeItem();
				g = new IconView("icons/result.png");
				t = di.getLocalName();
				getStyleClass().add("tree-cell-misc");
			}
			setGraphic(g);
			setText(t);
		}
	}
}