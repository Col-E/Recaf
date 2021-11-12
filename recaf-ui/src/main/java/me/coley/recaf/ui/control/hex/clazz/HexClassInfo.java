package me.coley.recaf.ui.control.hex.clazz;

import javafx.scene.control.Tab;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.ui.control.hex.HexView;
import me.coley.recaf.ui.util.Icons;

import java.util.Map;

/**
 * A side panel that displays an offset ranges of a class file.
 *
 * @author Matt Coley
 */
public class HexClassInfo {
	private final BorderPane wrapper = new BorderPane();
	private final HexView view;
	private ClassOffsetMap map;

	/**
	 * @param view
	 * 		Parent component.
	 */
	public HexClassInfo(HexView view) {
		this.view = view;
	}

	/**
	 * @return Tab containing class info panel.
	 */
	public Tab createClassInfoTab() {
		Tab tab = new Tab("Class");
		tab.setGraphic(Icons.getIconView(Icons.CODE));
		tab.setContent(wrapper);
		return tab;
	}

	/**
	 * Called when the class {@code byte[]} changes in the parent hex view.
	 *
	 * @param map
	 * 		New offset map.
	 */
	public void onUpdate(ClassOffsetMap map) {
		this.map = map;
		TreeView<ClassOffsetInfo> tree = new TreeView<>();
		tree.setShowRoot(false);
		tree.setCellFactory(c -> new TreeCell<ClassOffsetInfo>() {
			@Override
			protected void updateItem(ClassOffsetInfo item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setGraphic(null);
				} else {
					setGraphic(ClassInfoFormatter.format(view, item));
				}
			}
		});
		wrapper.setCenter(tree);
		TreeItem<ClassOffsetInfo> root = new TreeItem<>();
		for (Map.Entry<Integer, ClassOffsetInfo> entry : map.getMap().entrySet()) {
			build(root, entry.getValue());
		}
		tree.setRoot(root);
	}

	/**
	 * @return Offset map.
	 */
	public ClassOffsetMap getOffsetMap() {
		return map;
	}

	private void build(TreeItem<ClassOffsetInfo> root, ClassOffsetInfo info) {
		TreeItem<ClassOffsetInfo> item = new TreeItem<>(info);
		root.getChildren().add(item);
		for (ClassOffsetInfo subInfo : info.getChildren())
			build(item, subInfo);
	}
}
