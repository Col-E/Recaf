package me.coley.recaf.ui.component;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import me.coley.event.Bus;
import me.coley.recaf.bytecode.Hierarchy;
import me.coley.recaf.bytecode.Hierarchy.LoadStatus;
import me.coley.recaf.config.impl.ConfASM;
import me.coley.recaf.event.InsnOpenEvent;
import me.coley.recaf.event.MethodRenameEvent;
import me.coley.recaf.ui.component.AccessButton.AccessContext;
import me.coley.recaf.util.Lang;

/**
 * RefectiveItem decorator for allowing editing of MethodNode attributes.
 * 
 * @author Matt
 */
public class ReflectiveMethodNodeItem extends ReflectiveClassNodeItem {
	private final ClassNode methodOwner;
	private final MethodNode method;

	public ReflectiveMethodNodeItem(ClassNode methodOwner, MethodNode owner, Field field, String categoryKey,
			String translationKey) {
		super(owner, field, categoryKey, translationKey);
		this.methodOwner = methodOwner;
		this.method = owner;

	}

	@Override
	protected Class<?> getEditorType() {
		// check if proper type exists
		ParameterizedType type = getGenericType();
		if (type == null || type.getRawType() == null) {
			// custom editor for access / version
			if (getField().getName().equals("access")) {
				return AccessEditor.class;
			} else if (getField().getName().equals("instructions")) {
				return OpcodeEditor.class;
			} else if (getField().getName().equals("name")) {
				return MethodNameEditor.class;
			}
			// TODO: Annotation data
			return null;
		}
		// check raw-type for list
		if (!type.getRawType().equals(List.class)) {
			return null;
		}
		// Create custom editor for different argument types.
		Type arg = type.getActualTypeArguments()[0];
		if (arg.equals(String.class)) {
			// exceptions
			return Exceptions.class;
		} else if (arg.equals(TryCatchBlockNode.class)) {
			// exceptions
			return TryCatches.class;
		} else if (arg.equals(LocalVariableNode.class)) {
			// variables
			return LocalVars.class;
		} else if (arg.equals(ParameterNode.class)) {
			// parameters
			return Parameters.class;
		} else if (arg.equals(AnnotationNode.class) || arg.equals(TypeAnnotationNode.class)) {
			// annotations will eventually go here
			return AnnotationListEditor.class;
		}
		return null;
	}

	@Override
	protected ClassNode getNode() {
		return methodOwner;
	}

	/**
	 * Editor for editing interface list, as {@code List<String>}.
	 * 
	 * @author Matt
	 *
	 * @param <T>
	 *            {@code List<String>}
	 */
	public static class AccessEditor<T extends Integer> extends StagedCustomEditor<T> {
		public AccessEditor(Item item) {
			super(item);
		}

		@Override
		public Node getEditor() {
			return new AccessButton(AccessContext.METHOD, getValue().intValue()) {
				@SuppressWarnings("unchecked")
				@Override
				public void setAccess(int access) {
					super.setAccess(access);
					setValue((T) Integer.valueOf(access));
				}
			};
		}
	}

	/**
	 * String editor that also emits a FieldRenameEvent when the enter key is
	 * pressed.
	 * 
	 * @author Matt
	 *
	 * @param <T>
	 */
	public static class MethodNameEditor<T extends String> extends StagedCustomEditor<T> {
		public MethodNameEditor(Item item) {
			super(item);
		}

		@Override
		public Node getEditor() {
			ReflectiveMethodNodeItem refItem = (ReflectiveMethodNodeItem) item;
			ClassNode cn = refItem.getNode();
			MethodNode mn = (MethodNode) refItem.getOwner();
			TextField txtName = new TextField();
			txtName.setText(mn.name);
			ConfASM conf = ConfASM.instance();

			if (conf.useLinkedMethodRenaming()) {
				if (Hierarchy.getStatus() != LoadStatus.METHODS) {
					txtName.setDisable(true);
					txtName.setTooltip(new Tooltip(Lang.get("asm.edit.linkedmethods.notfinished")));
				} else if (conf.doLockLibraryMethod() && Hierarchy.isLocked(cn.name, mn.name, mn.desc)) {
					txtName.setDisable(true);
					txtName.setTooltip(new Tooltip(Lang.get("asm.edit.locklibmethods.locked")));
				}
			}
			txtName.setOnAction(e -> rename(cn, mn, txtName));
			return txtName;
		}

		private void rename(ClassNode owner, MethodNode method, TextField txtName) {
			String text = txtName.getText();
			if (!txtName.isDisabled() && !text.equals(method.name)) {
				// use disable property to prevent-double send
				txtName.setDisable(true);
				// send update
				Bus.post(new MethodRenameEvent(owner, method, method.name, text));
			}
		}
	}

	public static class OpcodeEditor<T extends InsnList> extends StagedCustomEditor<T> {
		public OpcodeEditor(Item item) {
			super(item);
		}

		@Override
		public Node getEditor() {
			ReflectiveMethodNodeItem refItem = (ReflectiveMethodNodeItem) item;
			InsnList list = (InsnList) item.getValue();

			return new ActionButton(Lang.get("misc.edit"), () -> {
				if (list.size() > 0) {
					Bus.post(new InsnOpenEvent(refItem.methodOwner, refItem.method, list.getFirst()));
				}
			});
		}
	}

	/**
	 * Editor for editing exceptions list, as {@code List<String>}.
	 * 
	 * @author Matt
	 *
	 * @param <T>
	 *            {@code List<String>}
	 */
	public static class Exceptions<T extends List<String>> extends StagedCustomEditor<T> {
		public Exceptions(Item item) {
			super(item);
		}

		@Override
		public Node getEditor() {
			return new ActionButton(Lang.get("misc.edit"), () -> open(this));
		}

		/**
		 * Open another window to handle editing of the value.
		 * 
		 * @param exceptions
		 *            CustomEditor instance to for value get/set callbacks.
		 */
		@SuppressWarnings("unchecked")
		private void open(Exceptions<T> exceptions) {
			if (staged()) {
				return;
			}
			BorderPane listPane = new BorderPane();
			BorderPane menuPane = new BorderPane();
			ListView<String> view = new ListView<>();
			ObservableList<String> list = FXCollections.observableArrayList(exceptions.getValue());
			list.addListener((ListChangeListener<String>) c -> setValue((T) list));
			view.setOnKeyPressed(new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent event) {
					if (event.getCode().equals(KeyCode.DELETE)) {
						delete(view);
					}
				}
			});
			view.setItems(list);
			view.setEditable(true);
			ContextMenu contextMenu = new ContextMenu();
			contextMenu.getItems().add(new ActionMenuItem(Lang.get("misc.remove"), () -> delete(view)));
			view.setContextMenu(contextMenu);
			listPane.setCenter(view);
			listPane.setBottom(menuPane);
			TextField newInterface = new TextField();
			newInterface.setOnAction((e) -> add(newInterface, view));
			Button addInterface = new ActionButton(Lang.get("misc.add"), () -> add(newInterface, view));
			menuPane.setCenter(newInterface);
			menuPane.setRight(addInterface);
			setStage("ui.bean.method.exceptions.name", listPane, 300, 500);
		}

		/**
		 * Add member in TextField to ListView.
		 * 
		 * @param text
		 * @param view
		 */
		private void add(TextField text, ListView<String> view) {
			view.itemsProperty().get().add(text.textProperty().get());
			text.textProperty().setValue("");
		}

		/**
		 * Remove selected items from ListView.
		 * 
		 * @param view
		 */
		private void delete(ListView<String> view) {
			MultipleSelectionModel<String> selection = view.selectionModelProperty().getValue();
			for (int index : selection.getSelectedIndices()) {
				view.getItems().remove(index);
			}
		}
	}

	/**
	 * Editor for editing try-catch blocks as {@code List<TryCatchBlockNode>}.
	 * 
	 * @author Matt
	 *
	 * @param <T>
	 *            {@code List<TryCatchBlockNode>}
	 */
	public static class TryCatches<T extends List<TryCatchBlockNode>> extends StagedCustomEditor<T> {

		public TryCatches(Item item) {
			super(item);
		}

		@Override
		public Node getEditor() {
			return new ActionButton(Lang.get("misc.edit"), () -> open(this));
		}

		/**
		 * Open another window to handle editing of the value.
		 * 
		 * @param catches
		 *            CustomEditor instance to for value get/set callbacks.
		 */
		private void open(TryCatches<T> catches) {
			if (staged()) {
				return;
			}
			MethodNode method = (MethodNode) item.getOwner();
			BorderPane listPane = new BorderPane();
			HBox menuPane = new HBox();
			ListView<TryCatchBlockNode> view = new ListView<>();
			ObservableList<TryCatchBlockNode> list = FXCollections.observableArrayList(catches.getValue());
			list.addListener(new ListChangeListener<TryCatchBlockNode>() {
				@SuppressWarnings("unchecked")
				@Override
				public void onChanged(Change<? extends TryCatchBlockNode> c) {
					setValue((T) list);
				}
			});
			view.setOnKeyPressed(new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent event) {
					if (event.getCode().equals(KeyCode.DELETE)) {
						delete(view);
					}
				}
			});
			view.setCellFactory(cell -> new ListCell<TryCatchBlockNode>() {
				@Override
				protected void updateItem(TryCatchBlockNode node, boolean empty) {
					super.updateItem(node, empty);
					if (empty || node == null) {
						setGraphic(null);
					} else {
						setGraphic(new TryConstructor(method, view, node));
					}
				}
			});
			view.setItems(list);
			view.setEditable(true);
			ContextMenu contextMenu = new ContextMenu();
			contextMenu.getItems().add(new ActionMenuItem(Lang.get("misc.remove"), () -> delete(view)));
			view.setContextMenu(contextMenu);
			listPane.setCenter(view);
			listPane.setBottom(menuPane);
			// Inputs for new value
			TryConstructor constr = new TryConstructor(method, view);
			Button addTry = new ActionButton(Lang.get("misc.add"), () -> constr.finish());
			menuPane.getChildren().addAll(constr, addTry);
			setStage("ui.bean.method.trycatchblocks.name", listPane, 600, 600);
		}

		/**
		 * Remove selected items from ListView.
		 * 
		 * @param view
		 */
		private void delete(ListView<TryCatchBlockNode> view) {
			MultipleSelectionModel<TryCatchBlockNode> selection = view.selectionModelProperty().getValue();
			for (int index : selection.getSelectedIndices()) {
				view.getItems().remove(index);
			}
		}

		/**
		 * Try-Catch representation via label-buttons and textfield for the
		 * caught type.
		 * 
		 * @author Matt
		 */
		private static class TryConstructor extends Constructor<TryCatchBlockNode> {
			private final ListView<TryCatchBlockNode> view;
			private final LabelButton start, end, handler;
			private final TextField type;

			private TryConstructor(MethodNode method, ListView<TryCatchBlockNode> view) {
				this.view = view;
				HBox menuPane = new HBox();
				start = new LabelButton(Lang.get("ui.bean.method.trycatchblocks.start"), method);
				end = new LabelButton(Lang.get("ui.bean.method.trycatchblocks.end"), method);
				handler = new LabelButton(Lang.get("ui.bean.method.trycatchblocks.handler"), method);
				type = new TextField();
				start.setTooltip(new Tooltip(Lang.get("ui.bean.method.trycatchblocks.start.tooltip")));
				end.setTooltip(new Tooltip(Lang.get("ui.bean.method.trycatchblocks.end.tooltip")));
				handler.setTooltip(new Tooltip(Lang.get("ui.bean.method.trycatchblocks.handler.tooltip")));
				type.setTooltip(new Tooltip(Lang.get("ui.bean.method.trycatchblocks.type.tooltip")));
				type.setOnAction(e -> finish());
				menuPane.getChildren().addAll(start, end, handler, type);
				setCenter(menuPane);
			}

			private TryConstructor(MethodNode method, ListView<TryCatchBlockNode> view, TryCatchBlockNode node) {
				this(method, view);
				start.setLabel(node.start);
				end.setLabel(node.end);
				handler.setLabel(node.handler);
				start.setUpdateTask(l -> node.start = l);
				end.setUpdateTask(l -> node.end = l);
				handler.setUpdateTask(l -> node.handler = l);
				type.setText(node.type);
				type.setOnKeyPressed(e -> {
					node.type = getTypeText();
				});
				type.setOnAction(e -> {});
			}

			@Override
			protected TryCatchBlockNode get() {
				// ASM wants type to be null to catch anything,

				return new TryCatchBlockNode(start.getLabel(), end.getLabel(), handler.getLabel(), getTypeText());
			}

			@Override
			protected void reset() {
				start.clear();
				end.clear();
				handler.clear();
				type.setText("");
			}

			@Override
			public void finish() {
				view.getItems().add(get());
				reset();
			}

			private String getTypeText() {
				String text = type.getText();
				if (text.isEmpty()) {
					text = null;
				}
				return text;
			}

		}
	}

	/**
	 * Editor for editing local variable table as
	 * {@code List<LocalVariableNode>}.
	 * 
	 * @author Matt
	 *
	 * @param <T>
	 *            {@code List<LocalVariableNode>}
	 */
	public static class LocalVars<T extends List<LocalVariableNode>> extends StagedCustomEditor<T> {

		public LocalVars(Item item) {
			super(item);
		}

		@Override
		public Node getEditor() {
			return new ActionButton(Lang.get("misc.edit"), () -> open(this));
		}

		/**
		 * Open another window to handle editing of the value.
		 * 
		 * @param vars
		 *            CustomEditor instance to for value get/set callbacks.
		 */
		private void open(LocalVars<T> vars) {
			if (staged()) {
				return;
			}
			MethodNode method = (MethodNode) item.getOwner();
			BorderPane listPane = new BorderPane();
			HBox menuPane = new HBox();
			ListView<LocalVariableNode> view = new ListView<>();
			T value = vars.getValue();
			value.sort(new Comparator<LocalVariableNode>() {
				@Override
				public int compare(LocalVariableNode o1, LocalVariableNode o2) {
					return Integer.compare(o1.index, o2.index);
				}
			});
			ObservableList<LocalVariableNode> list = FXCollections.observableArrayList(value);
			list.addListener(new ListChangeListener<LocalVariableNode>() {
				@SuppressWarnings("unchecked")
				@Override
				public void onChanged(Change<? extends LocalVariableNode> c) {
					setValue((T) list);
				}
			});
			view.setOnKeyPressed(new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent event) {
					if (event.getCode().equals(KeyCode.DELETE)) {
						delete(view);
					}
				}
			});
			view.setCellFactory(cell -> new ListCell<LocalVariableNode>() {
				@Override
				protected void updateItem(LocalVariableNode node, boolean empty) {
					super.updateItem(node, empty);
					if (empty || node == null) {
						setGraphic(null);
					} else {
						setGraphic(new VarConstructor(method, view, node));
					}
				}
			});
			view.setItems(list);
			view.setEditable(true);
			ContextMenu contextMenu = new ContextMenu();
			contextMenu.getItems().add(new ActionMenuItem(Lang.get("misc.remove"), () -> delete(view)));
			view.setContextMenu(contextMenu);
			listPane.setCenter(view);
			listPane.setBottom(menuPane);
			// Inputs for new value
			VarConstructor vari = new VarConstructor(method, view);
			Button addTry = new ActionButton(Lang.get("misc.add"), () -> vari.finish());
			menuPane.getChildren().addAll(vari, addTry);
			setStage("ui.bean.method.localvariables.name", listPane, 800, 600);
		}

		/**
		 * Remove selected items from ListView.
		 * 
		 * @param view
		 */
		private void delete(ListView<LocalVariableNode> view) {
			MultipleSelectionModel<LocalVariableNode> selection = view.selectionModelProperty().getValue();
			for (int index : selection.getSelectedIndices()) {
				view.getItems().remove(index);
			}
		}

		/**
		 * Local variable representation via a bunch of inputs.
		 * 
		 * @author Matt
		 */
		private static class VarConstructor extends Constructor<LocalVariableNode> {
			private final ListView<LocalVariableNode> view;
			private final TextField name, desc, signature, index;
			private final LabelButton start, end;
			private final MethodNode method;

			private VarConstructor(MethodNode method, ListView<LocalVariableNode> view) {
				this.view = view;
				this.method = method;
				HBox menuPane = new HBox();
				index = new TextField();
				name = new TextField();
				desc = new TextField();
				signature = new TextField();
				start = new LabelButton(Lang.get("ui.bean.method.localvariable.start"), method);
				end = new LabelButton(Lang.get("ui.bean.method.localvariable.end"), method);
				index.setTooltip(new Tooltip(Lang.get("ui.bean.method.localvariable.index.tooltip")));
				name.setTooltip(new Tooltip(Lang.get("ui.bean.method.localvariable.name.tooltip")));
				desc.setTooltip(new Tooltip(Lang.get("ui.bean.method.localvariables.desc.tooltip")));
				signature.setTooltip(new Tooltip(Lang.get("ui.bean.method.localvariables.signature.tooltip")));
				index.setOnAction(e -> name.requestFocus());
				name.setOnAction(e -> desc.requestFocus());
				desc.setOnAction(e -> signature.requestFocus());
				signature.setOnAction(e -> signature.requestFocus());
				// numeric-only textfield.
				index.textProperty().addListener(new ChangeListener<String>() {
					@Override
					public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
						if (!newValue.matches("\\d*")) {
							index.setText(newValue.replaceAll("[^\\d]", ""));
						}
						if (newValue.isEmpty() || index.getText().isEmpty()) {
							index.setText("0");
						}
					}
				});
				menuPane.getChildren().addAll(index, name, desc, signature, start, end);
				setCenter(menuPane);
			}

			private VarConstructor(MethodNode method, ListView<LocalVariableNode> view, LocalVariableNode variable) {
				this(method, view);
				start.setLabel(variable.start);
				end.setLabel(variable.end);
				index.setText(String.valueOf(variable.index));
				name.setText(variable.name);
				desc.setText(variable.desc);
				signature.setText(variable.signature);
				start.setUpdateTask(l -> variable.start = l);
				end.setUpdateTask(l -> variable.end = l);
				index.setOnKeyPressed(e -> {
					variable.index = Integer.parseInt(index.getText());
				});
				name.setOnKeyPressed(e -> {
					variable.name = name.getText();
				});
				desc.setOnKeyPressed(e -> {
					variable.desc = desc.getText();
				});
				signature.setOnKeyPressed(e -> {
					variable.signature = getSignature();
				});
			}

			@Override
			protected LocalVariableNode get() {
				int i = Integer.parseInt(index.getText());
				return new LocalVariableNode(name.getText(), desc.getText(), getSignature(), start.getLabel(), end.getLabel(), i);
			}

			@Override
			protected void reset() {
				index.setText(String.valueOf(method.localVariables.size()));
				name.setText("");
				desc.setText("");
				signature.setText("");
				start.setLabel(null);
				end.setLabel(null);
			}

			@Override
			public void finish() {
				view.getItems().add(get());
			}

			private String getSignature() {
				String text = signature.getText();
				if (text.isEmpty()) {
					text = null;
				}
				return text;
			}
		}
	}

	/**
	 * Editor for editing local variable table as
	 * {@code List<LocalVariableNode>}.
	 * 
	 * @author Matt
	 *
	 * @param <T>
	 *            {@code List<LocalVariableNode>}
	 */
	public static class Parameters<T extends List<ParameterNode>> extends StagedCustomEditor<T> {

		public Parameters(Item item) {
			super(item);
		}

		@Override
		public Node getEditor() {
			return new ActionButton(Lang.get("misc.edit"), () -> open(this));
		}

		/**
		 * Open another window to handle editing of the value.
		 * 
		 * @param params
		 *            CustomEditor instance to for value get/set callbacks.
		 */
		@SuppressWarnings("unchecked")
		private void open(Parameters<T> params) {
			if (staged()) {
				return;
			}
			MethodNode method = (MethodNode) item.getOwner();
			BorderPane listPane = new BorderPane();
			HBox menuPane = new HBox();
			ListView<ParameterNode> view = new ListView<>();
			T value = params.getValue();
			if (value == null) {
				value = (T) new ArrayList<ParameterNode>();
				method.parameters = value;
			}
			ObservableList<ParameterNode> list = FXCollections.observableArrayList(value);
			list.addListener(new ListChangeListener<ParameterNode>() {
				@Override
				public void onChanged(Change<? extends ParameterNode> c) {
					setValue((T) list);
				}
			});
			view.setOnKeyPressed(new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent event) {
					if (event.getCode().equals(KeyCode.DELETE)) {
						delete(view);
					}
				}
			});
			view.setCellFactory(cell -> new ListCell<ParameterNode>() {
				@Override
				protected void updateItem(ParameterNode node, boolean empty) {
					super.updateItem(node, empty);
					if (empty || node == null) {
						setGraphic(null);
					} else {
						setGraphic(new ParamConstructor(method, view, node));
					}
				}
			});
			view.setItems(list);
			view.setEditable(true);
			ContextMenu contextMenu = new ContextMenu();
			contextMenu.getItems().add(new ActionMenuItem(Lang.get("misc.remove"), () -> delete(view)));
			view.setContextMenu(contextMenu);
			listPane.setCenter(view);
			listPane.setBottom(menuPane);
			// Inputs for new value
			ParamConstructor param = new ParamConstructor(method, view);
			Button addTry = new ActionButton(Lang.get("misc.add"), () -> param.finish());
			menuPane.getChildren().addAll(param, addTry);
			setStage("ui.bean.method.parameters.name", listPane, 600, 600);
		}

		/**
		 * Remove selected items from ListView.
		 * 
		 * @param view
		 */
		private void delete(ListView<ParameterNode> view) {
			MultipleSelectionModel<ParameterNode> selection = view.selectionModelProperty().getValue();
			for (int index : selection.getSelectedIndices()) {
				view.getItems().remove(index);
			}
		}

		/**
		 * Parameter representation via a textfield and access-button.
		 * 
		 * @author Matt
		 */
		private static class ParamConstructor extends Constructor<ParameterNode> {
			private final ListView<ParameterNode> view;
			private final TextField name;
			private final AccessButton access;

			private ParamConstructor(MethodNode method, ListView<ParameterNode> view) {
				this.view = view;
				HBox menuPane = new HBox();
				access = new AccessButton(AccessContext.PARAM);
				name = new TextField();
				name.setTooltip(new Tooltip(Lang.get("ui.bean.method.localvariable.name.tooltip")));
				menuPane.getChildren().addAll(access, name);
				setCenter(menuPane);
			}

			private ParamConstructor(MethodNode method, ListView<ParameterNode> view, ParameterNode variable) {
				this(method, view);
				access.setAccess(variable.access);
				access.setUpdateTask(a -> variable.access = a);
				name.setText(variable.name);
				name.setOnKeyPressed(e -> variable.name = name.getText());
			}

			@Override
			protected ParameterNode get() {
				return new ParameterNode(name.getText(), access.getAccess());
			}

			@Override
			protected void reset() {
				access.setAccess(0);
				name.setText("");
			}

			@Override
			public void finish() {
				view.getItems().add(get());
			}
		}
	}
}
