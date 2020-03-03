package me.coley.recaf.ui.controls.tree;

import com.google.common.base.Joiner;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import me.coley.recaf.Recaf;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.ContextBuilder;
import me.coley.recaf.ui.controls.IconView;
import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.util.UiUtil;
import me.coley.recaf.workspace.JavaResource;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static me.coley.recaf.ui.ContextBuilder.menu;

/**
 * Cell renderer.
 *
 * @author Matt
 */
public class JavaResourceCell extends TreeCell {
	private static Map<Class<?>, Consumer<JavaResourceCell>> CLASS_TO_THING = new HashMap<>();

	@Override
	@SuppressWarnings("unchecked")
	public void updateItem(Object item, boolean empty) {
		super.updateItem(item, empty);
		if (empty) {
			setGraphic(null);
			setText(null);
		} else {
			Class<?> k = getTreeItem().getClass();
			Consumer<JavaResourceCell> populator = CLASS_TO_THING.get(k);
			if (populator != null)
				populator.accept(this);
		}
	}

	private static GuiController getController() {
		return (GuiController) Recaf.getController();
	}

	static {
		// Root cells
		CLASS_TO_THING.put(RootItem.class, cell -> {
			String text = cell.getTreeItem().getValue().toString();
			Node g = new IconView(UiUtil.getResourceIcon((JavaResource) cell.getTreeItem().getValue()));
			cell.getStyleClass().add("tree-cell-root");
			cell.setGraphic(g);
			cell.setText(text);
		});
		CLASS_TO_THING.put(SearchRootItem.class, cell -> {
			SearchRootItem sri = (SearchRootItem) cell.getTreeItem();
			String text = "Search[" + Joiner.on(", ").withKeyValueSeparator("=").join(sri.getParams())  + "] - " +
					sri.getResults().size() + " results";
			Node g = new IconView(UiUtil.getResourceIcon((JavaResource) cell.getTreeItem().getValue()));
			cell.getStyleClass().add("tree-cell-root");
			cell.setGraphic(g);
			cell.setText(text);
		});
		// Draw sub-roots (classes/files)
		CLASS_TO_THING.put(ClassFolderItem.class, cell -> {
			BaseItem b = (BaseItem) cell.getTreeItem();
			int count = b.resource().getClasses().size();
			String text = String.format("classes (%d)", count);
			Node g = new IconView("icons/folder-source.png");
			cell.getStyleClass().add("tree-cell-root-classes");
			cell.setGraphic(g);
			cell.setText(text);
		});
		CLASS_TO_THING.put(FileFolderItem.class, cell -> {
			BaseItem b = (BaseItem) cell.getTreeItem();
			int count = b.resource().getFiles().size();
			String text = String.format("files (%d)", count);
			Node g = new IconView("icons/folder-resource.png");
			cell.getStyleClass().add("tree-cell-root-files");
			cell.setGraphic(g);
			cell.setText(text);
		});
		// Draw classes
		CLASS_TO_THING.put(ClassItem.class, cell -> {
			ClassItem ci = (ClassItem) cell.getTreeItem();
			int access = ClassUtil.getAccess(ci.resource().getClasses().get(ci.getClassName()));
			String text = ci.getLocalName();
			Node g = UiUtil.createClassGraphic(access);
			cell.getStyleClass().add("tree-cell-class");
			cell.setContextMenu(menu().controller(getController()).ofClass(ci.getClassName()));
			cell.setGraphic(g);
			cell.setText(text);
		});
		// Draw class members
		CLASS_TO_THING.put(MemberItem.class, cell -> {
			MemberItem mi = (MemberItem) cell.getTreeItem();
			String owner = ((ClassItem) mi.getParent()).getClassName();
			ContextBuilder menu = menu().controller(getController());
			String text = mi.getLocalName();
			Node g = null;
			if(mi.isField()) {
				g = UiUtil.createFieldGraphic(mi.getMemberAccess());
				cell.setContextMenu(menu.ofField(owner, mi.getMemberName(), mi.getMemberDesc()));
			} else {
				g = UiUtil.createMethodGraphic(mi.getMemberAccess());
				cell.setContextMenu(menu.ofMethod(owner, mi.getMemberName(), mi.getMemberDesc()));
			}
			cell.getStyleClass().add("tree-cell-member");
			cell.setGraphic(g);
			cell.setText(text);
		});
		// Draw annotations
		CLASS_TO_THING.put(InsnItem.class, cell -> {
			InsnItem ii = (InsnItem) cell.getTreeItem();
			// TODO: Graphical representation instead to allow syntax highlighting
			String text = ii.getLocalName();
			Node g = new IconView("icons/result.png");
			cell.getStyleClass().add("tree-cell-instruction");
			// TODO: Instruction menu options:
			//  - Depends on insn type?
			cell.setGraphic(g);
			cell.setText(text);
		});
		// Draw method instructions
		CLASS_TO_THING.put(AnnoItem.class, cell -> {
			AnnoItem ai = (AnnoItem) cell.getTreeItem();
			String text = ai.getLocalName();
			Node g = new IconView("icons/class/annotation.png");
			cell.getStyleClass().add("tree-cell-annotation");
			cell.setContextMenu(menu().controller(getController()).ofClass(ai.getAnnoName()));
			cell.setGraphic(g);
			cell.setText(text);
		});
		// Draw method local variables
		CLASS_TO_THING.put(LocalItem.class, cell -> {
			LocalItem li = (LocalItem) cell.getTreeItem();
			String desc = li.getLocal().getDescriptor();
			Type type = Type.getType(desc);
			String className = type.getInternalName();
			int access = ClassUtil.getAccess(li.resource().getClasses().get(className));
			String text = "LOCAL[" + li.getLocal().getIndex() + "] " + li.getLocalName() + " - " +
					li.getLocal().getDescriptor();
			Node g = UiUtil.createClassGraphic(access);
			cell.getStyleClass().add("tree-cell-local");
			cell.setContextMenu(menu().controller(getController()).ofClass(className));
			cell.setGraphic(g);
			cell.setText(text);
		});
		// Draw method catch blocks
		CLASS_TO_THING.put(CatchItem.class, cell -> {
			CatchItem ci = (CatchItem) cell.getTreeItem();
			String className = ci.getCatchType();
			int access = ClassUtil.getAccess(ci.resource().getClasses().get(className));
			String text = "CATCH " + className;
			Node g = UiUtil.createClassGraphic(access);
			cell.getStyleClass().add("tree-cell-catch");
			cell.setContextMenu(menu().controller(getController()).ofClass(className));
			cell.setGraphic(g);
			cell.setText(text);
		});
		// Draw files
		CLASS_TO_THING.put(FileItem.class, cell -> {
			FileItem fi = (FileItem) cell.getTreeItem();
			String text = fi.getLocalName();
			Node g = UiUtil.createFileGraphic(fi.getLocalName());
			// TODO: Context menu for files
			cell.getStyleClass().add("tree-cell-file");
			cell.setGraphic(g);
			cell.setText(text);
		});
		// Draw directories/folders
		CLASS_TO_THING.put(DirectoryItem.class, cell -> {
			DirectoryItem di = (DirectoryItem) cell.getTreeItem();
			String text = di.getLocalName();
			Node g = new IconView("icons/class/package-flat.png");
			cell.getStyleClass().add("tree-cell-directory");
			cell.setGraphic(g);
			cell.setText(text);
		});
		// Draw misc
		CLASS_TO_THING.put(MiscItem.class, cell -> {
			DirectoryItem di = (DirectoryItem) cell.getTreeItem();
			String text = di.getLocalName();
			Node g = new IconView("icons/result.png");
			cell.getStyleClass().add("tree-cell-misc");
			cell.setGraphic(g);
			cell.setText(text);
		});
	}
}