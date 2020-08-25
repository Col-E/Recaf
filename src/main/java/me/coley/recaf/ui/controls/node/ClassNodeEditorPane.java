package me.coley.recaf.ui.controls.node;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.plugin.PluginsManager;
import me.coley.recaf.plugin.api.ClassVisitorPlugin;
import me.coley.recaf.ui.controls.ClassEditor;
import me.coley.recaf.ui.controls.view.ClassViewport;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.UiUtil;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static me.coley.recaf.util.LangUtil.translate;
import static me.coley.recaf.ui.ContextBuilder.menu;

/**
 * Editor for {@link ClassNode}.
 *
 * @author Matt
 */
public class ClassNodeEditorPane extends TabPane implements ClassEditor {
	private final GuiController controller;
	private final TableView<FieldNode> fields = new TableView<>();
	private final TableView<MethodNode> methods = new TableView<>();
	private final GridPane classInfoTable = new GridPane();
	private ClassNode node;
	private Tab tabClass;
	private Tab tabFields;
	private Tab tabMethods;

	/**
	 * @param controller
	 * 		Controller to act on.
	 * @param node
	 * 		Node instance to work off of.
	 */
	public ClassNodeEditorPane(GuiController controller, ClassNode node) {
		this.controller = controller;
		this.node = node;
		setup();
	}

	/**
	 * Setup tables.
	 */
	public void setup() {
		setupClass();
		setupFields();
		setupMethods();
	}

	/**
	 * Refresh tables.
	 *
	 * @param node
	 * 		Updated node.
	 */
	public void refresh(ClassNode node) {
		this.node = node;
		ObservableList<FieldNode> src = FXCollections.observableArrayList(this.node.fields);
		ObservableList<MethodNode> src2 = FXCollections.observableArrayList(this.node.methods);
		Platform.runLater(() -> {
			fields.setItems(src);
			methods.setItems(src2);
		});
	}

	private void setupClass() {
		// TODO: Edit class attributes
		// getTabs().add(tabClass = new Tab(translate("ui.edit.tab.classinfo"), classInfoTable));
	}

	private void setupFields() {
		ObservableList<FieldNode> src = FXCollections.observableArrayList(node.fields);
		fields.setItems(src);
		TableColumn<FieldNode, Integer> colIndex = new TableColumn<>(translate("ui.edit.tab.fields.index"));
		TableColumn<FieldNode, Integer> colAcc = new TableColumn<>(translate("ui.edit.tab.fields.access"));
		TableColumn<FieldNode, Type> colType = new TableColumn<>(translate("ui.edit.tab.fields.type"));
		TableColumn<FieldNode, String> colName = new TableColumn<>(translate("ui.edit.tab.fields.name"));
		colIndex.setCellValueFactory(c -> new SimpleObjectProperty<>(src.indexOf(c.getValue()) + 1));
		colAcc.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().access));
		colAcc.setCellFactory(col -> new TableCell<FieldNode, Integer>(){
			@Override
			public void updateItem(Integer item, boolean empty) {
				super.updateItem(item, empty);
				setText(null);
				if(empty) {
					setGraphic(null);
				} else {
					setGraphic(UiUtil.createFieldGraphic(item));
					setTooltip(new Tooltip(AccessFlag.getApplicableFlags(AccessFlag.Type.FIELD).stream()
							.filter(flag -> (flag.getMask() & item) == flag.getMask())
							.map(AccessFlag::getName)
							.collect(Collectors.joining(", "))));
				}
			}
		});
		colType.setCellValueFactory(c -> new SimpleObjectProperty<>(Type.getType(c.getValue().desc)));
		colType.setCellFactory(col -> new TableCell<FieldNode, Type>(){
			@Override
			public void updateItem(Type item, boolean empty) {
				super.updateItem(item, empty);
				setGraphic(null);
				if(empty) {
					setText(null);
				} else {
					Type used = item;
					int arrayLevel = item.getSort() == Type.ARRAY ? item.getDimensions() : 0;
					if (arrayLevel > 0)
						used = used.getElementType();
					String argType = used.getInternalName();
					if(argType.indexOf('/') > 0) {
						argType = argType.substring(argType.lastIndexOf('/') + 1);
					} else if(used.getSort() <= Type.DOUBLE) {
						argType = used.getClassName();
					}
					setText(argType + array(arrayLevel));
					setTooltip(new Tooltip(item.getDescriptor()));
				}
			}
		});
		colName.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().name));
		fields.getColumns().add(colIndex);
		fields.getColumns().add(colAcc);
		fields.getColumns().add(colType);
		fields.getColumns().add(colName);
		fields.setRowFactory(t -> new TableRow<FieldNode>(){
			@Override
			protected void updateItem(FieldNode item, boolean empty) {
				super.updateItem(item, empty);
				if(item != null)
					setContextMenu(menu()
							.controller(controller)
							.view((ClassViewport) ClassNodeEditorPane.this.getParent())
							.declaration(true)
							.ofField(node.name, item.name, item.desc));
				else
					setContextMenu(null);
			}
		});
		getTabs().add(tabFields = new Tab(translate("ui.edit.tab.fields"), fields));
	}

	private void setupMethods() {
		ObservableList<MethodNode> src = FXCollections.observableArrayList(node.methods);
		methods.setItems(src);
		TableColumn<MethodNode, Integer> colIndex = new TableColumn<>(translate("ui.edit.tab.fields.index"));
		TableColumn<MethodNode, Integer> colAcc = new TableColumn<>(translate("ui.edit.tab.methods.access"));
		TableColumn<MethodNode, Type> colType = new TableColumn<>(translate("ui.edit.tab.methods.return"));
		TableColumn<MethodNode, String> colName = new TableColumn<>(translate("ui.edit.tab.methods.name"));
		TableColumn<MethodNode, Type> colArgs = new TableColumn<>(translate("ui.edit.tab.methods.args"));
		colIndex.setCellValueFactory(c -> new SimpleObjectProperty<>(src.indexOf(c.getValue()) + 1));
		colAcc.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().access));
		colAcc.setCellFactory(col -> new TableCell<MethodNode, Integer>(){
			@Override
			public void updateItem(Integer item, boolean empty) {
				super.updateItem(item, empty);
				setText(null);
				if(empty) {
					setGraphic(null);
				} else {
					setGraphic(UiUtil.createMethodGraphic(item));
					setTooltip(new Tooltip(AccessFlag.getApplicableFlags(AccessFlag.Type.METHOD).stream()
							.filter(flag -> (flag.getMask() & item) == flag.getMask())
							.map(AccessFlag::getName)
							.collect(Collectors.joining(", "))));
				}
			}
		});
		colType.setCellValueFactory(c -> new SimpleObjectProperty<>(Type.getType(c.getValue().desc).getReturnType()));
		colType.setCellFactory(col -> new TableCell<MethodNode, Type>(){
			@Override
			public void updateItem(Type item, boolean empty) {
				super.updateItem(item, empty);
				setGraphic(null);
				if(empty) {
					setText(null);
				} else {
					Type used = item;
					int arrayLevel = item.getSort() == Type.ARRAY ? item.getDimensions() : 0;
					if (arrayLevel > 0)
						used = used.getElementType();
					String argType = used.getInternalName();
					if(argType.indexOf('/') > 0) {
						argType = argType.substring(argType.lastIndexOf('/') + 1);
					} else if(used.getSort() <= Type.DOUBLE) {
						argType = used.getClassName();
					}
					setText(argType + array(arrayLevel));
					setTooltip(new Tooltip(item.getDescriptor()));
				}
			}
		});
		colName.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().name));
		colArgs.setCellValueFactory(c -> new SimpleObjectProperty<>(Type.getType(c.getValue().desc)));
		colArgs.setCellFactory(col -> new TableCell<MethodNode, Type>(){
			@Override
			public void updateItem(Type item, boolean empty) {
				super.updateItem(item, empty);
				setGraphic(null);
				if(empty) {
					setText(null);
				} else {
					List<String> args = new ArrayList<>();
					for (Type arg : item.getArgumentTypes()) {
						Type used = arg;
						int arrayLevel = arg.getSort() == Type.ARRAY ? arg.getDimensions() : 0;
						if (arrayLevel > 0)
							used = used.getElementType();
						String argType = used.getInternalName();
						if(argType.indexOf('/') > 0) {
							argType = argType.substring(argType.lastIndexOf('/') + 1);
						} else if(used.getSort() <= Type.DOUBLE) {
							argType = used.getClassName();
						}
						args.add(argType + array(arrayLevel));
					}
					setText(String.join(", ", args));
					String descArgs = item.getDescriptor();
					descArgs = descArgs.substring(0, descArgs.indexOf(')') + 1);
					setTooltip(new Tooltip(descArgs));
				}
			}
		});
		methods.getColumns().add(colIndex);
		methods.getColumns().add(colAcc);
		methods.getColumns().add(colType);
		methods.getColumns().add(colName);
		methods.getColumns().add(colArgs);
		methods.setRowFactory(t -> new TableRow<MethodNode>(){
			@Override
			protected void updateItem(MethodNode item, boolean empty) {
				super.updateItem(item, empty);
				if(item != null)
					setContextMenu(menu()
							.controller(controller)
							.view((ClassViewport) ClassNodeEditorPane.this.getParent())
							.declaration(true)
							.ofMethod(node.name, item.name, item.desc));
			}
		});
		getTabs().add(tabMethods = new Tab(translate("ui.edit.tab.methods"), methods));
	}

	private static String array(int arrayLevel) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < arrayLevel; i++)
			sb.append("[]");
		return sb.toString();
	}

	@Override
	public Map<String, byte[]> save(String name) {
		ClassWriter cw = controller.getWorkspace().createWriter(ClassWriter.COMPUTE_FRAMES);
		ClassVisitor visitor = cw;
		for (ClassVisitorPlugin visitorPlugin : PluginsManager.getInstance()
				.ofType(ClassVisitorPlugin.class)) {
			visitor = visitorPlugin.intercept(visitor);
		}
		node.accept(visitor);
		return Collections.singletonMap(node.name, cw.toByteArray());
	}

	@Override
	public void selectMember(String name, String desc) {
		boolean method = desc.contains("(");
		int i = 0;
		TableView<?> target;
		if (method) {
			selectionModelProperty().get().select(tabMethods);
			for (MethodNode mn : methods.getItems()) {
				if (mn.name.equals(name) && mn.desc.equals(desc))
					break;
				i++;
			}
			target = methods;
		} else {
			selectionModelProperty().get().select(tabFields);
			for (FieldNode fn : fields.getItems()) {
				if (fn.name.equals(name) && fn.desc.equals(desc))
					break;
				i++;
			}
			target = fields;
		}
		if (i < target.getItems().size()) {
			int idx = i;
			Platform.runLater(() -> {
				target.requestFocus();
				target.getSelectionModel().select(idx);
				target.getFocusModel().focus(idx);
			});
		}
	}
}
