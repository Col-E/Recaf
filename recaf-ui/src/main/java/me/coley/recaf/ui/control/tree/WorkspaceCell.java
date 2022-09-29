package me.coley.recaf.ui.control.tree;

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ItemInfo;
import me.coley.recaf.ui.control.tree.item.BaseTreeItem;
import me.coley.recaf.ui.control.tree.item.BaseTreeValue;
import me.coley.recaf.ui.control.tree.item.ClassItem;
import me.coley.recaf.ui.control.tree.item.DexClassItem;
import me.coley.recaf.ui.control.tree.item.FieldItem;
import me.coley.recaf.ui.control.tree.item.FileItem;
import me.coley.recaf.ui.control.tree.item.MethodItem;
import me.coley.recaf.ui.util.CellFactory;
import me.coley.recaf.util.TextDisplayUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Cell for {@link BaseTreeValue} to represent items from a {@link Workspace}.
 *
 * @author Matt Coley
 */
public class WorkspaceCell extends TreeCell<BaseTreeValue> {
	private static final Logger logger = Logging.get(WorkspaceCell.class);
	private static final Map<Class<?>, BiFunction<Workspace, BaseTreeValue, ItemInfo>> INFO_FUNCS = new HashMap<>();
	private final CellOriginType cellOrigin;

	/**
	 * Create a new cell.
	 *
	 * @param cellOrigin
	 * 		Parent tree's type. May result in changes in context menu option availability.
	 */
	public WorkspaceCell(CellOriginType cellOrigin) {
		this.cellOrigin = cellOrigin;
	}

	private void onMouseClick(MouseEvent e) {
		TreeItem<?> item = getTreeItem();
		if (item == null) {
			logger.error("Failed to get tree item of current cell: {}", getText());
			return;
		}
		if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
			// Open selected
			if (item.isLeaf()) {
				WorkspaceTree.openItem(item);
			}
			// Recursively open children until multiple options are present
			else if (item.isExpanded()) {
				BaseTreeItem.recurseOpen(item);
			}
		}
	}

	@Override
	protected void updateItem(BaseTreeValue value, boolean empty) {
		super.updateItem(value, empty);
		// Initial reset (prevents recycled cells from propagating mouse actions)
		setGraphic(null);
		setContextMenu(null);
		setOnMouseClicked(null);
		// Populate based on active state
		if (empty) {
			// Always unbind if the cell is no longer needed.
			textProperty().unbind();
			setText(null);
			setGraphic(null);
			setContextMenu(null);
			setOnMouseClicked(null);
		} else {
			// Defaults
			if (textProperty().isBound()) {
				// TODO: Unbinding the cells a big aggressively here, but this seems to work for now
				textProperty().unbind();
			} else {
				setText(TextDisplayUtil.escapeLimit(value.getPathElementValue()));
			}
			// Populate based on associated info, or the item class
			BaseTreeItem item = value.getItem();
			Resource resource = item.getContainingResource();
			BiFunction<Workspace, BaseTreeValue, ItemInfo> infoLookup = INFO_FUNCS.get(item.getClass());
			if (infoLookup != null) {
				Workspace workspace = RecafUI.getController().getWorkspace();
				ItemInfo info = infoLookup.apply(workspace, value);
				CellFactory.update(cellOrigin, this, resource, info);
			} else {
				CellFactory.update(cellOrigin, this, resource, item);
			}
			setOnMouseClicked(this::onMouseClick);
		}
	}

	static {
		INFO_FUNCS.put(ClassItem.class, (workspace, v) -> {
			String className = ((ClassItem) v.getItem()).getClassName();
			return workspace.getResources().getClass(className);
		});
		INFO_FUNCS.put(DexClassItem.class, (workspace, v) -> {
			String className = ((DexClassItem) v.getItem()).getClassName();
			return workspace.getResources().getDexClass(className);
		});
		INFO_FUNCS.put(FileItem.class, (workspace, v) -> {
			String fileName = ((FileItem) v.getItem()).getFileName();
			return workspace.getResources().getFile(fileName);
		});
		INFO_FUNCS.put(FieldItem.class, (workspace, v) -> ((FieldItem) v.getItem()).getInfo());
		INFO_FUNCS.put(MethodItem.class, (workspace, v) -> ((MethodItem) v.getItem()).getInfo());
	}
}
