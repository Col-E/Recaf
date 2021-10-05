package me.coley.recaf.ui.control.tree;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ItemInfo;
import me.coley.recaf.ui.control.tree.item.*;
import me.coley.recaf.ui.panel.pe.PEExplorerPanel;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.ByteHeaderUtil;
import me.coley.recaf.ui.util.CellFactory;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import org.slf4j.Logger;

import javax.swing.border.Border;
import java.io.IOException;
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

	/**
	 * Open the content of the tree item in the UI.
	 *
	 * @param item
	 * 		Item representing value.
	 */
	private void openItem(TreeItem<?> item) {
		// TODO: Open classes/files
		/*
		if (item instanceof ClassItem) {
			ClassItem ci = (ClassItem) item;
			String name = ci.getClassName();
			// CLASS OPEN HERE
		} else if (item instanceof DexClassItem) {
			DexClassItem dci = (DexClassItem) item;
			String name = dci.getClassName();
			// CLASS OPEN HERE
		} else if (item instanceof FileItem) {
			FileItem ri = (FileItem) item;
			String name = ri.getFileName();
			// FILE OPEN HERE
		}
		*/

		if (item instanceof FileItem) {
			FileItem fi = (FileItem)item;
			// TODO: Replace this with "CommonUX" once this gets pushed to mainline develop branch
			try {
				byte[] image = RecafUI.getController().getWorkspace().getResources()
						.getFile(fi.getFileName()).getValue();
				if (ByteHeaderUtil.match(image, ByteHeaderUtil.EXE_DLL)) {
					RecafUI.getWindows().getMainWindow().getDockingRootPane()
							.createTab(fi.getFileName(), new PEExplorerPanel(image));
				}
			}
			catch (IOException e) {
				logger.error(e.getMessage());
			}
		}

	}

	@Override
	protected void updateItem(BaseTreeValue value, boolean empty) {
		super.updateItem(value, empty);
		if (empty) {
			setGraphic(null);
			setText(null);
			setContextMenu(null);
			setOnMouseClicked(null);
		} else {
			// Defaults
			setText(value.getPathElementValue());
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
