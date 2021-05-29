package me.coley.recaf.ui.controls.node;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.parse.bytecode.parser.ModifierParser;
import me.coley.recaf.plugin.PluginsManager;
import me.coley.recaf.plugin.api.ClassVisitorPlugin;
import me.coley.recaf.ui.controls.*;
import me.coley.recaf.ui.controls.pane.ColumnPane;
import me.coley.recaf.ui.controls.view.ClassViewport;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.UiUtil;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static me.coley.recaf.util.LangUtil.translate;
import static me.coley.recaf.ui.ContextBuilder.menu;
import static me.coley.recaf.util.ClassUtil.VERSION_OFFSET;

/**
 * Editor for {@link ClassNode}.
 *
 * @author Matt
 */
public class ClassNodeEditorPane extends TabPane implements ClassEditor {
	private final GuiController controller;
	private final TableView<FieldNode> fields = new TableView<>();
	private final TableView<MethodNode> methods = new TableView<>();
	private final ColumnPane classInfoTable = new ColumnPane(new Insets(5, 10, 5, 10), 38, 62, 5);
	private final Map<Supplier<Boolean>, Node> classInfoEditors = new HashMap<>();
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
		// TODO: Support the following
		//  - Module
		//  - nestHostClass / nestMembers
		//  - Permitted subclasses
		//  - Record component nodes
		//  - Annotations
		//  - Inner classes
		getTabs().add(tabClass = new Tab(translate("ui.edit.tab.classinfo"), classInfoTable));
		TextField txtName = new ActionTextField(controller, node.name, n -> {
			if (n.isEmpty())
				return false;
			// TODO: Should there be an option to remap this instead of just overriding?
			// TODO: This will have to re-open the editor with the correct updated path
			node.name = n;
			return true;
		});
		TextField txtSignature = new ActionTextField(controller, node.signature, n -> {
			if (n.isEmpty())
				node.signature = null;
			else
				node.signature = n;
			return true;
		});
		TextField txtSuper = new ActionTextField(controller, node.superName, n -> {
			if (n.isEmpty())
				node.superName = null;
			else
				node.superName = n;
			return true;
		});
		TextArea txtInterfaces = new ActionTextArea(controller, String.join("\n", node.interfaces), n -> {
			if (n.isEmpty())
				node.interfaces = Collections.emptyList();
			else
				node.interfaces = Arrays.asList(n.split("\\s+"));
			return true;
		});
		TextField txtSource = new ActionTextField(controller, node.sourceFile, n -> {
			if (n.isEmpty())
				node.sourceFile = null;
			else
				node.sourceFile = n;
			return true;
		});
		TextField txtSourceDebug = new ActionTextField(controller, node.sourceDebug, n -> {
			if (n.isEmpty())
				node.sourceDebug = null;
			else
				node.sourceDebug = n;
			return true;
		});
		TextField txtVersion = new ActionTextField(controller, String.valueOf(node.version - VERSION_OFFSET), n -> {
			try {
				int v = Integer.parseInt(n) + VERSION_OFFSET;
				if (v > Opcodes.V16 || v < 45)
					return false;
				node.version = v;
				return true;
			} catch (NumberFormatException ignored) {
				return false;
			}
		});
		TextField txtAccess = new ActionTextField(controller,
				Arrays.stream(AccessFlag.values())
						.filter(f -> f.getTypes().contains(AccessFlag.Type.CLASS))
						.filter(f -> (node.access & f.getMask()) == f.getMask())
						.map(AccessFlag::getName)
						.collect(Collectors.joining(" ")),
				n -> {
					try {
						int acc = 0;
						for (String arg : n.split("\\s+")) {
							if (arg.isEmpty())
								continue;
							acc |= new ModifierParser().visit(0, arg).getValue();
						}
						node.access = acc;
						return true;
					} catch (Throwable ignored) {
						return false;
					}
				});
		TextField txtOuterClass = new ActionTextField(controller, node.outerClass, n -> {
			if (n.isEmpty())
				node.outerClass = null;
			else
				node.outerClass = n;
			return true;
		});
		TextField txtOuterMethod = new ActionTextField(controller, node.outerMethod, n -> {
			if (n.isEmpty())
				node.outerMethod = null;
			else
				node.outerMethod = n;
			return true;
		});
		TextField txtOuterMethodDesc = new ActionTextField(controller, node.outerMethodDesc, n -> {
			if (n.isEmpty())
				node.outerMethodDesc = null;
			else
				node.outerMethodDesc = n;
			return true;
		});
		txtName.setDisable(true);
		addEditor("ui.bean.class.name", txtName);
		addEditor("ui.bean.class.signature", txtSignature);
		addEditor("ui.bean.class.supername", txtSuper);
		addEditor("ui.bean.class.interfaces", txtInterfaces);
		addEditor("ui.bean.class.version", txtVersion);
		addEditor("ui.bean.class.access", txtAccess);
		addEditor("ui.bean.class.sourcefile", txtSource);
		addEditor("ui.bean.class.sourcedebug", txtSourceDebug);
		addEditor("ui.bean.class.outerclass", txtOuterClass);
		addEditor("ui.bean.class.outermethod", txtOuterMethod);
		addEditor("ui.bean.class.outermethoddesc", txtOuterMethodDesc);
	}

	@SuppressWarnings("unchecked")
	private void addEditor(String key, Node node) {
		// SubLabeled label = new SubLabeled(translate(key + ".name"), translate(key + ".desc"), "bold");
		Label label = new Label(translate(key + ".name"));
		label.getStyleClass().add("bold");
		label.setTooltip(new Tooltip(translate(key + ".desc")));
		// Store save-action
		classInfoTable.add(label, node);
		Supplier<String>[] supplier = new Supplier[1];
		Predicate<String>[] predicate = new Predicate[1];
		if (node instanceof ActionTextField) {
			supplier[0] = ((ActionTextField) node)::getText;
			predicate[0] = ((ActionTextField) node).getAction();
		} else if (node instanceof ActionTextArea) {
			supplier[0] = ((ActionTextArea) node)::getText;
			predicate[0] = ((ActionTextArea) node).getAction();
		} else {
			throw new IllegalStateException("No editor for field: " + key);
		}
		classInfoEditors.put(() -> predicate[0].test(supplier[0].get()), node);
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
		colAcc.setCellFactory(col -> new TableCell<FieldNode, Integer>() {
			@Override
			public void updateItem(Integer item, boolean empty) {
				super.updateItem(item, empty);
				setText(null);
				if (empty) {
					setGraphic(null);
				} else {
					setGraphic(UiUtil.createFieldGraphic(item));
				}
			}
		});
		colType.setCellValueFactory(c -> new SimpleObjectProperty<>(Type.getType(c.getValue().desc)));
		colType.setCellFactory(col -> new TableCell<FieldNode, Type>() {
			@Override
			public void updateItem(Type item, boolean empty) {
				super.updateItem(item, empty);
				setGraphic(null);
				if (empty) {
					setText(null);
				} else {
					Type used = item;
					int arrayLevel = item.getSort() == Type.ARRAY ? item.getDimensions() : 0;
					if (arrayLevel > 0)
						used = used.getElementType();
					String argType = used.getInternalName();
					if (argType.indexOf('/') > 0) {
						argType = argType.substring(argType.lastIndexOf('/') + 1);
					} else if (used.getSort() <= Type.DOUBLE) {
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
		fields.setRowFactory(t -> new TableRow<FieldNode>() {
			@Override
			protected void updateItem(FieldNode item, boolean empty) {
				super.updateItem(item, empty);
				if (item != null)
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
		colAcc.setCellFactory(col -> new TableCell<MethodNode, Integer>() {
			@Override
			public void updateItem(Integer item, boolean empty) {
				super.updateItem(item, empty);
				setText(null);
				if (empty) {
					setGraphic(null);
				} else {
					setGraphic(UiUtil.createMethodGraphic(item));
				}
			}
		});
		colType.setCellValueFactory(c -> new SimpleObjectProperty<>(Type.getType(c.getValue().desc).getReturnType()));
		colType.setCellFactory(col -> new TableCell<MethodNode, Type>() {
			@Override
			public void updateItem(Type item, boolean empty) {
				super.updateItem(item, empty);
				setGraphic(null);
				if (empty) {
					setText(null);
				} else {
					Type used = item;
					int arrayLevel = item.getSort() == Type.ARRAY ? item.getDimensions() : 0;
					if (arrayLevel > 0)
						used = used.getElementType();
					String argType = used.getInternalName();
					if (argType.indexOf('/') > 0) {
						argType = argType.substring(argType.lastIndexOf('/') + 1);
					} else if (used.getSort() <= Type.DOUBLE) {
						argType = used.getClassName();
					}
					setText(argType + array(arrayLevel));
					setTooltip(new Tooltip(item.getDescriptor()));
				}
			}
		});
		colName.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().name));
		colArgs.setCellValueFactory(c -> new SimpleObjectProperty<>(Type.getType(c.getValue().desc)));
		colArgs.setCellFactory(col -> new TableCell<MethodNode, Type>() {
			@Override
			public void updateItem(Type item, boolean empty) {
				super.updateItem(item, empty);
				setGraphic(null);
				if (empty) {
					setText(null);
				} else {
					List<String> args = new ArrayList<>();
					for (Type arg : item.getArgumentTypes()) {
						Type used = arg;
						int arrayLevel = arg.getSort() == Type.ARRAY ? arg.getDimensions() : 0;
						if (arrayLevel > 0)
							used = used.getElementType();
						String argType = used.getInternalName();
						if (argType.indexOf('/') > 0) {
							argType = argType.substring(argType.lastIndexOf('/') + 1);
						} else if (used.getSort() <= Type.DOUBLE) {
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
		methods.setRowFactory(t -> new TableRow<MethodNode>() {
			@Override
			protected void updateItem(MethodNode item, boolean empty) {
				super.updateItem(item, empty);
				if (item != null)
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
		for (Map.Entry<Supplier<Boolean>, Node> entry : classInfoEditors.entrySet()) {
			// This will apply all editor actions. If any fail to apply their changes the save is aborted.
			if (!entry.getKey().get()) {
				UiUtil.animateFailure(entry.getValue(), 500);
				throw new IllegalStateException("Fix the invalid input");
			}
		}
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
