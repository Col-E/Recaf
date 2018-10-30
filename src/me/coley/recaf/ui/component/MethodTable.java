package me.coley.recaf.ui.component;

import java.util.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.scene.control.*;
import javafx.scene.input.*;
import me.coley.event.*;
import me.coley.recaf.bytecode.search.Parameter;
import me.coley.recaf.bytecode.TypeUtil;
import me.coley.recaf.event.*;
import me.coley.recaf.ui.*;
import me.coley.recaf.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * Table of MethodNodes.
 * 
 * @author Matt
 */
public class MethodTable extends TableView<MethodNode> {
	@SuppressWarnings("unchecked")
	public MethodTable(ClassNode owner, List<MethodNode> methods) {
		MethodTable info = this;
		setRowFactory(v -> {
			TableRow<MethodNode> row = new TableRow<>();
			row.setOnMouseClicked(e -> {
				// Double click or middle click to open method
				if ((e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) || (e.getButton() == MouseButton.MIDDLE)) {
					if (!row.isSelected()) getSelectionModel().select(row.getIndex());
					MethodNode mn = getSelectionModel().getSelectedItem();
					Bus.post(new MethodOpenEvent(owner, mn, info));
				}
			});
			return row;
		});
		getItems().addListener((ListChangeListener.Change<? extends MethodNode> c) -> {
			while (c.next()) {
				if (c.wasRemoved() || c.wasAdded()) {
					Bus.post(new ClassDirtyEvent(owner));
				}
			}
		});
		TableColumn<MethodNode, Integer> colFlags = new TableColumn<>(Lang.get("ui.edit.tab.methods.flags"));
		TableColumn<MethodNode, String> colName = new TableColumn<>(Lang.get("ui.edit.tab.methods.name"));
		TableColumn<MethodNode, Type> colRet = new TableColumn<>(Lang.get("ui.edit.tab.methods.return"));
		TableColumn<MethodNode, Type[]> colArgs = new TableColumn<>(Lang.get("ui.edit.tab.methods.args"));
		setFixedCellSize(20); // fixed cell height
		getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		getColumns().addAll(colFlags, colRet, colName, colArgs);
		colFlags.setCellValueFactory(cell -> JavaFX.observable(cell.getValue().access));
		colFlags.setCellFactory(cell -> new TableCell<MethodNode, Integer>() {
			@Override
			protected void updateItem(Integer flags, boolean empty) {
				super.updateItem(flags, empty);
				if (empty || flags == null) {
					setGraphic(null);
				} else {
					setGraphic(Icons.getMember(flags, true));
				}
			}
		});
		colFlags.setComparator(Integer::compare);
		colName.setCellValueFactory(cell -> JavaFX.observable(cell.getValue().name));
		colName.setCellFactory(cell -> new TableCell<MethodNode, String>() {
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
		colRet.setCellValueFactory(cell -> JavaFX.observable(Type.getType(cell.getValue().desc).getReturnType()));
		colRet.setCellFactory(cell -> new TableCell<MethodNode, Type>() {
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
		colArgs.setCellValueFactory(cell -> JavaFX.observable(Type.getType(cell.getValue().desc).getArgumentTypes()));
		colArgs.setCellFactory(cell -> new TableCell<MethodNode, Type[]>() {
			@Override
			protected void updateItem(Type[] types, boolean empty) {
				super.updateItem(types, empty);
				if (empty || types == null || types.length == 0) {
					setGraphic(null);
				} else {
					setGraphic(FormatFactory.typeArray(types));
				}
			}
		});
		colArgs.setComparator((o1, o2) -> {
			int len = Math.min(o1.length, o2.length);
			for (int i = 0; i < len; i++) {
				// Compare, ensure if descriptors are simplified
				// they are sorted properly to match displayed
				// results.
				int c = TypeUtil.filter(o1[i]).compareTo(TypeUtil.filter(o2[i]));
				if (c != 0) {
					return c;
				}
			}
			// in case of recurring matches
			return Integer.compare(o1.length, o2.length);
		});
		setItems(FXCollections.observableArrayList(methods));
		// context menu
		ContextMenu ctxBase = new ContextMenu();
		ContextMenu ctx = new ContextMenu();
		ctxBase.getItems().add(new ActionMenuItem(Lang.get("misc.add"), () -> {
			MethodNode mn = new MethodNode(0, "temp", "()V", null, null);
			methods.add(mn);
			getItems().add(mn);
		}));
		setContextMenu(ctxBase);
		ctx.getItems().add(new ActionMenuItem(Lang.get("ui.bean.method.instructions.name"), () -> {
			MethodNode mn = getSelectionModel().getSelectedItem();
			Bus.post(new InsnOpenEvent(owner, mn, null));
		}));
		ctx.getItems().add(new ActionMenuItem(Lang.get("ui.bean.class.decompile.name"), () -> {
			MethodNode mn = getSelectionModel().getSelectedItem();
			DecompileItem decomp = new DecompileItem(owner, mn);
			decomp.decompile();
		}));
		ctx.getItems().add(new ActionMenuItem(Lang.get("ui.search.reference"), () -> {
			MethodNode mn = getSelectionModel().getSelectedItem();
			FxSearch.open(Parameter.references(owner.name, mn.name, mn.desc));
		}));
		ctx.getItems().add(new ActionMenuItem(Lang.get("misc.add"), () -> {
			MethodNode mn = new MethodNode(0, "temp", "()V", null, null);
			methods.add(mn);
			getItems().add(mn);
		}));
		ctx.getItems().add(new ActionMenuItem(Lang.get("misc.remove"), () -> {
			int i = getSelectionModel().getSelectedIndex();
			methods.remove(i);
			getItems().remove(i);
		}));
		ctx.getItems().add(new ActionMenuItem(Lang.get("misc.duplicate"), () -> {
			MethodNode sel = getSelectionModel().getSelectedItem();
			String[] exceptions = sel.exceptions.toArray(new String[0]);
			MethodNode copy = new MethodNode(sel.access, sel.name + "_copy", sel.desc, sel.signature, exceptions);
			sel.accept(copy);
			methods.add(copy);
			getItems().add(copy);
		}));
		// only allow when item is selected
		getSelectionModel().selectedIndexProperty().addListener((c) -> {
			setContextMenu(getSelectionModel().getSelectedIndex() == -1 ? null : ctx);
		});
		// mark class as dirty when items list changes.
		getItems().addListener((ListChangeListener.Change<? extends MethodNode> c) -> {
			Bus.post(new ClassDirtyEvent(owner));
		});
	}
}
