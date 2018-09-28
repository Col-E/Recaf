package me.coley.recaf.ui.component;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.Opcodes;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
import me.coley.recaf.bytecode.AccessFlag;
import me.coley.recaf.event.ClassDirtyEvent;
import me.coley.recaf.event.ClassRenameEvent;
import me.coley.recaf.ui.component.ReflectivePropertySheet.ReflectiveItem;
import me.coley.recaf.util.Icons;
import me.coley.recaf.util.Lang;

/**
 * RefectiveItem decorator for allowing editing of ClassNode attributes.
 * 
 * @author Matt
 */
public class ReflectiveClassNodeItem extends ReflectiveItem {
	public ReflectiveClassNodeItem(Object owner, Field field, String categoryKey, String translationKey) {
		super(owner, field, categoryKey, translationKey);
	}

	@Override
	protected Class<?> getEditorType() {
		// check if proper type exists
		ParameterizedType type = getGenericType();
		if (type == null || type.getRawType() == null) {
			// custom editor for access / version
			if (getField().getName().equals("access")) {
				return AccessEditor.class;
			} else if (getField().getName().equals("version")) {
				return VersionEditor.class;
			} else if (getField().getName().equals("name")) {
				return ClassNameEditor.class;
			}
			// TODO: implement ModuleNode editor
			/*
			 * if (getField().getName().equals("module")) { // for ModuleNode
			 * return (Class<? extends CustomEditor<T>>) ModuleEditor.class; }
			 */
			return null;
		}
		// check raw-type for list
		if (!type.getRawType().equals(List.class)) {
			return null;
		}
		// Create custom editor for different argument types.
		Type arg = type.getActualTypeArguments()[0];
		if (arg.equals(String.class)) {
			// interfaces
			return InterfaceList.class;
		} else if (arg.equals(InnerClassNode.class)) {
			// inner classes
			return InnerClassList.class;
		} else if (arg.equals(AnnotationNode.class)) {
			// annotation lists
			return AnnotationListEditor.class;
		} else if (arg.equals(TypeAnnotationNode.class)) {
			// type-annotation lists
			return AnnotationTypeListEditor.class;
		}
		return null;
	}

	@Override
	public void setValue(Object value) {
		// Only save if this is not being called as an init-phase of javafx
		// displaying content. Once the UI is loaded editing works as intended.
		if (checkCaller() && !value.equals(getValue())) {
			super.setValue(value);
			Bus.post(new ClassDirtyEvent(getNode()));
		}
	}

	/**
	 * @return ClassNode containing the field being modified.
	 */
	protected ClassNode getNode() {
		return (ClassNode) getOwner();
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
			return new AccessButton(AccessFlag.Type.CLASS, getValue().intValue()) {
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
	 * String editor that also emits a ClassRenameEvent when the enter key is
	 * pressed.
	 * 
	 * @author Matt
	 *
	 * @param <T>
	 */
	public static class ClassNameEditor<T extends String> extends StagedCustomEditor<T> {
		public ClassNameEditor(Item item) {
			super(item);
		}

		@Override
		public Node getEditor() {
			ReflectiveClassNodeItem refItem = (ReflectiveClassNodeItem) item;
			ClassNode cn = refItem.getNode();
			TextField txtName = new TextField();
			txtName.setText(cn.name);
			txtName.setOnAction(e -> rename(cn, txtName));
			// This works for when focus is lost, but I'm not sure if thats user
			// friendly...
			// If you type anything in and click anywhere else (or close the
			// tab) it will
			// do the rename action.
			//@formatter:off
			/*
			txtName.focusedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> focused, Boolean oldVal, Boolean newVal) {
					if (!newVal) {
						rename(cn, txtName);
					}
				}
			});
			*/
			//@formatter:on
			return txtName;
		}

		private void rename(ClassNode node, TextField txtName) {
			String text = txtName.getText();
			if (!txtName.isDisabled() && !text.equals(node.name)) {
				Bus.post(new ClassRenameEvent(node, node.name, text));
				// use disable property to prevent-double send
				txtName.setDisable(true);
			}
		}
	}

	/**
	 * Editor for editing interface list, as {@code List<String>}.
	 * 
	 * @author Matt
	 *
	 * @param <T>
	 *            {@code List<String>}
	 */
	public static class VersionEditor<T extends Integer> extends StagedCustomEditor<T> {
		public VersionEditor(Item item) {
			super(item);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Node getEditor() {
			ComboBox<JavaVersion> combo = new ComboBox<>(FXCollections.observableArrayList(JavaVersion.values()));
			combo.valueProperty().addListener((ov, prev, current) -> setValue((T) Integer.valueOf(current.version)));
			combo.setValue(JavaVersion.get(getValue()));
			return combo;
		}

		/**
		 * Enumeration of supported java versions.
		 * 
		 * @author Matt
		 */
		private static enum JavaVersion {
			Java5(Opcodes.V1_5), Java6(Opcodes.V1_6), Java7(Opcodes.V1_7), Java8(Opcodes.V1_8), Java9(Opcodes.V9), Java10(
					Opcodes.V10), Java11(Opcodes.V11);
			private final int version;

			JavaVersion(int version) {
				this.version = version;

			}

			/**
			 * Map of java version values to the enum representation.
			 */
			private static final Map<Integer, JavaVersion> lookup = new HashMap<>();

			/**
			 * Lookup in {@link #lookup version-to-enum} mao.
			 * 
			 * @param version
			 * @return
			 */
			public static JavaVersion get(int version) {
				return lookup.get(version);
			}

			static {
				// populate lookup map
				for (JavaVersion version : values()) {
					lookup.put(version.version, version);
				}
			}
		}
	}

	/**
	 * Editor for editing interface list, as {@code List<String>}.
	 * 
	 * @author Matt
	 *
	 * @param <T>
	 *            {@code List<String>}
	 */
	public static class InterfaceList<T extends List<String>> extends StagedCustomEditor<T> {
		public InterfaceList(Item item) {
			super(item);
		}

		@Override
		public Node getEditor() {
			return new ActionButton(Lang.get("misc.edit"), () -> open(this));
		}

		/**
		 * Open another window to handle editing of the value.
		 * 
		 * @param interfaceList
		 *            CustomEditor instance to for value get/set callbacks.
		 */
		private void open(InterfaceList<T> interfaceList) {
			if (staged()) {
				return;
			}
			BorderPane listPane = new BorderPane();
			BorderPane menuPane = new BorderPane();
			ListView<String> view = new ListView<>();
			ObservableList<String> list = FXCollections.observableArrayList(interfaceList.getValue());
			list.addListener(new ListChangeListener<String>() {
				@SuppressWarnings("unchecked")
				@Override
				public void onChanged(Change<? extends String> c) {
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
			setStage("ui.bean.class.interfaces.name", listPane, 300, 500);
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
	 * Editor for editing interface list, as {@code List<String>}.
	 * 
	 * @author Matt
	 *
	 * @param <T>
	 *            {@code List<String>}
	 */
	public static class InnerClassList<T extends List<InnerClassNode>> extends StagedCustomEditor<T> {

		public InnerClassList(Item item) {
			super(item);
		}

		@Override
		public Node getEditor() {
			return new ActionButton(Lang.get("misc.edit"), () -> open(this));
		}

		/**
		 * Open another window to handle editing of the value.
		 * 
		 * @param innerClassList
		 *            CustomEditor instance to for value get/set callbacks.
		 */
		private void open(InnerClassList<T> innerClassList) {
			if (staged()) {
				return;
			}
			BorderPane listPane = new BorderPane();
			HBox menuPane = new HBox();
			ListView<InnerClassNode> view = new ListView<>();
			ObservableList<InnerClassNode> list = FXCollections.observableArrayList(innerClassList.getValue());
			list.addListener(new ListChangeListener<InnerClassNode>() {
				@SuppressWarnings("unchecked")
				@Override
				public void onChanged(Change<? extends InnerClassNode> c) {
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
			view.setCellFactory(cell -> new ListCell<InnerClassNode>() {
				@Override
				protected void updateItem(InnerClassNode node, boolean empty) {
					super.updateItem(node, empty);
					if (empty || node == null) {
						setGraphic(null);
						setText(null);
					} else {
						setGraphic(Icons.getMember(node.access, true));
						if (node.innerName == null || node.innerName.isEmpty()) {
							setText(node.name.substring(node.name.lastIndexOf("/") + 1));
						} else {
							setText(node.innerName);
						}
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
			TextField name = new TextField();
			TextField innerName = new TextField();
			TextField outerName = new TextField();
			name.setTooltip(new Tooltip(Lang.get("ui.bean.class.innerclasses.name.tooltip")));
			innerName.setTooltip(new Tooltip(Lang.get("ui.bean.class.innerclasses.inner.tooltip")));
			outerName.setTooltip(new Tooltip(Lang.get("ui.bean.class.innerclasses.outer.tooltip")));
			AccessButton access = new AccessButton(AccessFlag.Type.INNER_CLASS);
			name.setOnAction((e) -> innerName.selectAll());
			innerName.setOnAction((e) -> outerName.selectAll());
			outerName.setOnAction((e) -> add(access, name, innerName, outerName, view));
			Button addInterface = new ActionButton(Lang.get("misc.add"), () -> add(access, name, innerName, outerName, view));
			menuPane.getChildren().addAll(access, name, innerName, outerName, addInterface);
			setStage("ui.bean.class.innerclasses.name", listPane, 530, 400);
		}

		/**
		 * Add member from input fields to ListView.
		 * 
		 * @param access
		 * @param name
		 * @param innerName
		 * @param outerName
		 * @param view
		 */
		private void add(AccessButton access, TextField name, TextField innerName, TextField outerName,
				ListView<InnerClassNode> view) {
			view.getItems().add(new InnerClassNode(name.getText(), outerName.getText(), innerName.getText(), access.getAccess()));
			access.setAccess(0);
			name.textProperty().setValue("");
			innerName.textProperty().setValue("");
			outerName.textProperty().setValue("");
		}

		/**
		 * Remove selected items from ListView.
		 * 
		 * @param view
		 */
		private void delete(ListView<InnerClassNode> view) {
			MultipleSelectionModel<InnerClassNode> selection = view.selectionModelProperty().getValue();
			for (int index : selection.getSelectedIndices()) {
				view.getItems().remove(index);
			}
		}
	}
}
