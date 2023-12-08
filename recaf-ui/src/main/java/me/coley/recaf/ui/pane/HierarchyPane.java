package me.coley.recaf.ui.pane;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.graph.InheritanceGraph;
import me.coley.recaf.graph.InheritanceVertex;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.behavior.Updatable;
import me.coley.recaf.ui.context.ContextBuilder;
import me.coley.recaf.ui.control.ActionButton;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.TextDisplayUtil;
import me.coley.recaf.util.Translatable;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.FxThreadUtil;
import org.slf4j.Logger;


/**
 * Visualization of the class hierarchy for children and parent relations.
 *
 * @author Matt Coley
 */
public class HierarchyPane extends BorderPane implements Updatable<CommonClassInfo> {
	private static final Logger logger = Logging.get(HierarchyPane.class);
	private final HierarchyTree tree = new HierarchyTree();
	private HierarchyMode mode = HierarchyMode.PARENTS;
	private CommonClassInfo info;

	/**
	 * New hierarchy panel.
	 */
	public HierarchyPane() {
		setCenter(tree);
		setBottom(createModeBar());
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		info = newValue;
		tree.onUpdate(newValue);
	}

	private Node createModeBar() {
		HBox wrapper = new HBox();
		Button btnChild = new ActionButton(Lang.getBinding("menu.view.hierarchy.children"), () -> {
			mode = HierarchyMode.CHILDREN;
			onUpdate(info);
			wrapper.getChildren().get(0).setDisable(true);
			wrapper.getChildren().get(1).setDisable(false);
		});
		Button btnParent = new ActionButton(Lang.getBinding("menu.view.hierarchy.parents"), () -> {
			mode = HierarchyMode.PARENTS;
			onUpdate(info);
			wrapper.getChildren().get(0).setDisable(false);
			wrapper.getChildren().get(1).setDisable(true);
		});
		// Initial disabled state
		if (mode == HierarchyMode.PARENTS) {
			btnParent.setDisable(true);
		} else {
			btnChild.setDisable(true);
		}
		btnChild.setGraphic(Icons.getIconView(Icons.CHILDREN, 32));
		btnParent.setGraphic(Icons.getIconView(Icons.PARENTS, 32));
		wrapper.getChildren().add(btnChild);
		wrapper.getChildren().add(btnParent);
		return wrapper;
	}

	/**
	 * Mode for switching between displaying parents and children relations.
	 */
	private enum HierarchyMode implements Translatable {
		CHILDREN, PARENTS;

		@Override
		public String getTranslationKey() {
			if (this == CHILDREN) {
				return "hierarchy.children";
			} else {
				return "hierarchy.parents";
			}
		}

		@Override
		public String toString() {
			return Lang.get(getTranslationKey());
		}
	}

	/**
	 * Tree that represents the hierarchy of a class.
	 */
	class HierarchyTree extends TreeView<CommonClassInfo> implements Updatable<CommonClassInfo> {
		private HierarchyTree() {
			getStyleClass().add("transparent-tree");
			setCellFactory(param -> new HierarchyCell());
		}

		@Override
		public void onUpdate(CommonClassInfo newValue) {
			InheritanceGraph graph = RecafUI.getController().getServices().getInheritanceGraph();
			TreeItem<CommonClassInfo> root = new TreeItem<>(newValue);
			String name = newValue.getName();
			InheritanceVertex vertex = graph.getVertex(name);
			if (vertex != null) {
				if (mode == HierarchyMode.PARENTS) {
					createParents(root, vertex);
				} else if (mode == HierarchyMode.CHILDREN) {
					createChildren(root, vertex);
				}
			} else {
				logger.warn("Vertex lookup failed for class '{}'", name);
			}
			FxThreadUtil.run(() -> setRoot(root));
		}

		private void createParents(TreeItem<CommonClassInfo> root, InheritanceVertex rootVertex) {
			root.setExpanded(true);
			for (InheritanceVertex parentVertex : rootVertex.getParents()) {
				if (parentVertex.getName().equals("java/lang/Object"))
					continue;
				TreeItem<CommonClassInfo> subItem = new TreeItem<>(parentVertex.getValue());
				if (noLoops(root, subItem)) {
					root.getChildren().add(subItem);
					createParents(subItem, parentVertex);
				}
			}
		}

		private void createChildren(TreeItem<CommonClassInfo> root, InheritanceVertex rootVertex) {
			root.setExpanded(true);
			for (InheritanceVertex childVertex : rootVertex.getChildren()) {
				TreeItem<CommonClassInfo> subItem = new TreeItem<>(childVertex.getValue());
				if (noLoops(root, subItem)) {
					root.getChildren().add(subItem);
					createChildren(subItem, childVertex);
				}
			}
		}

		private boolean noLoops(TreeItem<?> root, TreeItem<?> child) {
			if (root == null)
				return true;
			// HierarchyItem implements a hashCode based on class names
			// So any cycle should trigger this and we'll abort.
			if (root.hashCode() == child.hashCode())
				return false;
			return noLoops(root.getParent(), child);
		}
	}

	/**
	 * Cell of a class in the hierarchy.
	 */
	static class HierarchyCell extends TreeCell<CommonClassInfo> {
		private EventHandler<MouseEvent> onClickFilter;

		private HierarchyCell() {
			getStyleClass().add("transparent-cell");
			getStyleClass().add("monospace");
		}

		@Override
		protected void updateItem(CommonClassInfo item, boolean empty) {
			super.updateItem(item, empty);
			setDisclosureNode(null);
			if (empty || item == null) {
				setText(null);
				setGraphic(null);
				setOnMouseClicked(null);
				if (onClickFilter != null)
					removeEventFilter(MouseEvent.MOUSE_PRESSED, onClickFilter);
			} else {
				setGraphic(Icons.getClassIcon(item));
				setText(TextDisplayUtil.shortenEscapeLimit(item.getName()));
				// Menu based on info subtype
				if (item instanceof ClassInfo) {
					setContextMenu(ContextBuilder.forClass((ClassInfo) item).build());
				} else if (item instanceof DexClassInfo) {
					setContextMenu(ContextBuilder.forDexClass((DexClassInfo) item).build());
				}
				// Override the double click behavior to open the class. Doesn't work using the "setOn..." methods.
				onClickFilter = (MouseEvent e) -> {
					if (e.getClickCount() >= 2 && e.getButton().equals(MouseButton.PRIMARY)) {
						e.consume();
						CommonUX.openClass(item);
					}
				};
				addEventFilter(MouseEvent.MOUSE_PRESSED, onClickFilter);
			}
		}
	}
}
