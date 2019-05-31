package me.coley.recaf.ui.component;

import java.util.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.scene.control.*;
import javafx.scene.input.*;
import me.coley.event.*;
import me.coley.recaf.bytecode.search.Parameter;
import me.coley.recaf.bytecode.TypeUtil;
import me.coley.recaf.bytecode.search.StringMode;
import me.coley.recaf.event.*;
import me.coley.recaf.ui.*;
import me.coley.recaf.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * Table of FieldNodes.
 * 
 * @author Matt
 */
public class FieldTable extends TableView<FieldNode> {
	@SuppressWarnings("unchecked")
	public FieldTable(ClassNode owner, List<FieldNode> fields) {
		FieldTable info = this;
		setRowFactory(v -> {
			TableRow<FieldNode> row = new TableRow<>();
			row.setOnMouseClicked(e -> {
				// Double click or middle click to open field
				if ((e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) || (e.getButton() == MouseButton.MIDDLE)) {
					if (!row.isSelected()) getSelectionModel().select(row.getIndex());
					FieldNode fn = getSelectionModel().getSelectedItem();
					Bus.post(new FieldOpenEvent(owner, fn, info));
				}
			});
			return row;
		});
		getItems().addListener((ListChangeListener.Change<? extends FieldNode> c) -> {
			while (c.next()) {
				if (c.wasRemoved() || c.wasAdded()) {
					Bus.post(new ClassDirtyEvent(owner));
				}
			}
		});
		TableColumn<FieldNode, Integer> colFlags = new TableColumn<>(Lang.get("ui.edit.tab.fields.flags"));
		TableColumn<FieldNode, String> colName = new TableColumn<>(Lang.get("ui.edit.tab.fields.name"));
		TableColumn<FieldNode, Type> colRet = new TableColumn<>(Lang.get("ui.edit.tab.fields.type"));
		getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		setFixedCellSize(20); // fixed cell height
		getColumns().addAll(colFlags, colRet, colName);
		colFlags.setCellValueFactory(cell -> JavaFX.observable(cell.getValue().access));
		colFlags.setCellFactory(cell -> new TableCell<FieldNode, Integer>() {
			@Override
			protected void updateItem(Integer flags, boolean empty) {
				super.updateItem(flags, empty);
				if (empty || flags == null) {
					setGraphic(null);
				} else {
					setGraphic(Icons.getMember(flags, false));
				}
			}
		});
		colFlags.setComparator(Integer::compare);
		colName.setCellValueFactory(cell -> JavaFX.observable(cell.getValue().name));
		colName.setCellFactory(cell -> new TableCell<FieldNode, String>() {
			@Override
			protected void updateItem(String name, boolean empty) {
				super.updateItem(name, empty);
				if (empty || name == null) {
					setGraphic(null);
				} else {
					setGraphic(FormatFactory.name(name));
				}
			}
		});
		colName.setComparator(Comparator.comparing(String::toString));
		colRet.setCellValueFactory(cell -> JavaFX.observable(Type.getType(cell.getValue().desc)));
		colRet.setCellFactory(cell -> new TableCell<FieldNode, Type>() {
			@Override
			protected void updateItem(Type type, boolean empty) {
				super.updateItem(type, empty);
				if (empty || type == null) {
					setGraphic(null);
				} else {
					setGraphic(FormatFactory.type(type));
				}
			}
		});
		// Compare, ensure if descriptors are simplified
		// they are sorted properly to match displayed results.
		colRet.setComparator(Comparator.comparing(TypeUtil::filter));
		setItems(FXCollections.observableArrayList(fields));
		// context menu
		ContextMenu ctxBase = new ContextMenu();
		ContextMenu ctx = new ContextMenu();
		ctxBase.getItems().add(new ActionMenuItem(Lang.get("misc.add"), () -> {
			FieldNode fn = new FieldNode(0, "temp", "I", null, null);
			fields.add(fn);
			getItems().add(fn);
		}));
		setContextMenu(ctxBase);
		ctx.getItems().add(new ActionMenuItem(Lang.get("ui.search.reference"), () -> {
			FieldNode fn = getSelectionModel().getSelectedItem();
			Parameter p = Parameter.references(owner.name, fn.name, fn.desc);
			p.setStringMode(StringMode.EQUALITY);
			FxSearch.open(p);
		}));
		ctx.getItems().add(new ActionMenuItem(Lang.get("misc.add"), () -> {
			FieldNode fn = new FieldNode(0, "temp", "I", null, null);
			fields.add(fn);
			getItems().add(fn);
		}));
		ctx.getItems().add(new ActionMenuItem(Lang.get("misc.remove"), () -> {
			int i = getSelectionModel().getSelectedIndex();
			fields.remove(i);
			getItems().remove(i);
		}));
		// only allow when item is selected
		getSelectionModel().selectedIndexProperty().addListener((c) -> {
			setContextMenu(getSelectionModel().getSelectedIndex() == -1 ? null : ctx);
		});
		// mark class as dirty when items list changes.
		getItems().addListener((ListChangeListener.Change<? extends FieldNode> c) -> {
			Bus.post(new ClassDirtyEvent(owner));
		});
	}
}
