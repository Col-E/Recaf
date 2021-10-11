package me.coley.recaf.ui.control.hex.clazz;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import me.coley.cafedude.ConstPool;
import me.coley.cafedude.attribute.Attribute;
import me.coley.cafedude.constant.CpClass;
import me.coley.cafedude.constant.CpUtf8;
import me.coley.recaf.ui.control.hex.EditableHexLocation;
import me.coley.recaf.ui.control.hex.HexView;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.NodeUtil;
import me.coley.recaf.util.AccessFlag;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
					setGraphic(forInfo(item));
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

	private void build(TreeItem<ClassOffsetInfo> root, ClassOffsetInfo info) {
		TreeItem<ClassOffsetInfo> item = new TreeItem<>(info);
		root.getChildren().add(item);
		for (ClassOffsetInfo subInfo : info.getChildren())
			build(item, subInfo);
	}

	private GridPane forInfo(ClassOffsetInfo info) {
		Object value = info.getValue();
		Label title = new Label(info.getType().name());
		if (info.getType() == ClassOffsetInfoType.ATTRIBUTE) {
			Attribute attr = (Attribute) value;
			String name = info.getClassFile().getPool().getUtf(attr.getNameIndex());
			title.setText(title.getText() + ": " + name);
		}
		Label range = new Label(HexView.offsetStr(info.getStart()) + " - " + HexView.offsetStr(info.getEnd()));
		title.getStyleClass().add("b");
		title.getStyleClass().add("monospace");
		range.getStyleClass().add("monospace");
		int row = 0;
		GridPane content = new GridPane();
		content.addRow(row++, title);
		content.addRow(row++, range);
		content.setOnMouseEntered(e -> NodeUtil.addStyleClass(content, "hex-hover"));
		content.setOnMouseExited(e -> NodeUtil.removeStyleClass(content, "hex-hover"));
		content.setOnMouseClicked(e -> {
			int start = info.getStart();
			int end = info.getEnd();
			view.centerOffset(start);
			view.onDragStart(EditableHexLocation.RAW, start);
			view.onDragUpdate(end);
			view.onDragEnd();
		});

		ConstPool cp = info.getClassFile().getPool();
		switch (info.getType()) {
			case MINOR_VERSION:
			case MAJOR_VERSION:
			case CONSTANT_POOL_COUNT:
			case INTERFACES_COUNT:
			case FIELDS_COUNT:
			case METHODS_COUNT:
			case ATTRIBUTES_COUNT:
			case FIELD_ATTRIBUTES_COUNT:
			case METHOD_ATTRIBUTES_COUNT:
			case ATTRIBUTE_LENGTH:
			case CODE_MAX_STACK:
			case CODE_MAX_LOCALS:
			case CODE_LENGTH:
			case CODE_EXCEPTIONS_TABLE_LENGTH: {
				content.addRow(row++, dim(value));
				break;
			}
			case ACCESS_FLAGS:
			case FIELD_ACC_FLAGS:
			case METHOD_ACC_FLAGS: {
				int acc = (int) value;
				AccessFlag.Type type = AccessFlag.Type.CLASS;
				if (info.getType() == ClassOffsetInfoType.FIELD_ACC_FLAGS) {
					type = AccessFlag.Type.FIELD;
				} else if (info.getType() == ClassOffsetInfoType.METHOD_ACC_FLAGS) {
					type = AccessFlag.Type.METHOD;
				}
				String flagNames = AccessFlag.getApplicableFlags(type, acc).stream()
						.map(AccessFlag::getName)
						.collect(Collectors.joining(" "));
				content.addRow(row++, dim(Integer.toString(acc, 2) + " -> " + flagNames));
				break;
			}
			case THIS_CLASS:
			case SUPER_CLASS: {
				int classIndex = (int) value;
				CpClass cpClass = (CpClass) cp.get(classIndex);
				CpUtf8 cpClassName = (CpUtf8) cp.get(cpClass.getIndex());
				content.addRow(row++, dim(classIndex + ": " + cpClassName.getText()));
				break;
			}
			case FIELD_NAME_INDEX:
			case FIELD_DESC_INDEX:
			case METHOD_NAME_INDEX:
			case METHOD_DESC_INDEX:
			case ATTRIBUTE_NAME_INDEX: {
				int utfIndex = (int) value;
				CpUtf8 cpUtf8 = (CpUtf8) cp.get(utfIndex);
				content.addRow(row++, dim(utfIndex + ": " + cpUtf8.getText()));
				break;
			}
			default:
				break;
		}

		return content;
	}

	private Node dim(Object value) {
		Label label = new Label(Objects.toString(value));
		label.getStyleClass().add("monospace");
		label.getStyleClass().add("faint");
		return label;
	}
}
