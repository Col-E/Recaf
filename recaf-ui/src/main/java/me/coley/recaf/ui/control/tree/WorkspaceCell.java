package me.coley.recaf.ui.control.tree;

import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import me.coley.recaf.RecafUI;
import me.coley.recaf.ui.control.IconView;
import me.coley.recaf.ui.control.tree.item.*;
import me.coley.recaf.util.Lang;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.ClassInfo;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Cell for {@link BaseTreeValue}.
 *
 * @author Matt Coley
 */
public class WorkspaceCell extends TreeCell<BaseTreeValue> {
	private static final Logger logger = Logging.get(WorkspaceCell.class);
	private static final Map<Class<?>, BiFunction<Workspace, BaseTreeValue, String>> TEXT_FUNCS = new HashMap<>();
	private static final Map<Class<?>, BiFunction<Workspace, BaseTreeValue, Node>> GRAPHIC_FUNCS = new HashMap<>();

	@Override
	protected void updateItem(BaseTreeValue value, boolean empty) {
		super.updateItem(value, empty);
		if (empty) {
			setGraphic(null);
			setText(null);
			setContextMenu(null);
		} else {
			setText(getValueText(value));
			setGraphic(getValueGraphic(value));
		}
	}

	private String getValueText(BaseTreeValue value) {
		BaseTreeItem item = value.getItem();
		// Apply function if available
		BiFunction<Workspace, BaseTreeValue, String> textFunction = TEXT_FUNCS.get(item.getClass());
		if (textFunction != null) {
			return textFunction.apply(workspace(), value);
		}
		// Just use element name.
		return value.getPathElementValue();
	}

	private Node getValueGraphic(BaseTreeValue value) {
		BaseTreeItem item = value.getItem();
		// Apply function if available
		BiFunction<Workspace, BaseTreeValue, Node> graphicFunction = GRAPHIC_FUNCS.get(item.getClass());
		if (graphicFunction != null) {
			return graphicFunction.apply(workspace(), value);
		}
		// No graphic
		return null;
	}

	/**
	 * Static accessor to current workspace.
	 * Used because cells are reused and cannot be given a per-instance workspace.
	 *
	 * @return Current workspace.
	 */
	private static Workspace workspace() {
		return RecafUI.getController().getWorkspace();
	}

	static {
		// Text
		TEXT_FUNCS.put(RootItem.class, (w, v) -> "Root");
		TEXT_FUNCS.put(DummyItem.class, (w, v) -> ((DummyItem) v.getItem()).getDummyText());
		TEXT_FUNCS.put(ResourceItem.class, (w, v) ->
				((ResourceItem) v.getItem()).getResource().getContentSource().toString());
		TEXT_FUNCS.put(ResourceClassesItem.class, (w, v) -> Lang.get("tree.classes"));
		TEXT_FUNCS.put(ResourceFilesItem.class, (w, v) -> Lang.get("tree.files"));
		// Icons
		GRAPHIC_FUNCS.put(ResourceClassesItem.class, (w, v) -> new IconView("icons/folder-source.png"));
		GRAPHIC_FUNCS.put(ResourceFilesItem.class, (w, v) -> new IconView("icons/folder-resource.png"));
		GRAPHIC_FUNCS.put(PackageItem.class, (w, v) -> new IconView("icons/folder-package.png"));
		GRAPHIC_FUNCS.put(DirectoryItem.class, (w, v) -> new IconView("icons/folder.png"));
		GRAPHIC_FUNCS.put(ClassItem.class, (w, v) -> {
			String className = ((ClassItem) v.getItem()).getClassName();
			ClassInfo info = w.getResources().getClass(className);
			if (info == null) {
				logger.error("Failed to lookup class for tree cell '{}'", className);
				return new IconView("icons/class/class.png");
			}
			// TODO: Cleanup access usage once access utility is added to project
			else if ((info.getAccess() & Opcodes.ACC_ANNOTATION) > 0) {
				return new IconView("icons/class/annotation.png");
			} else if ((info.getAccess() & Opcodes.ACC_INTERFACE) > 0) {
				return new IconView("icons/class/interface.png");
			} else if ((info.getAccess() & Opcodes.ACC_ENUM) > 0) {
				return new IconView("icons/class/enum.png");
			}
			return new IconView("icons/class/class.png");
		});
		GRAPHIC_FUNCS.put(FileItem.class, (w, v) -> {
			// TODO: Determine file type
			return new IconView("icons/binary.png");
		});
	}
}
