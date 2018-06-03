package me.coley.recaf.ui.component;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import me.coley.event.Bus;
import me.coley.recaf.bytecode.Access;
import me.coley.recaf.bytecode.TypeUtil;
import me.coley.recaf.event.ClassDirtyEvent;
import me.coley.recaf.ui.FormatFactory;
import me.coley.recaf.ui.component.ReflectivePropertySheet.CustomEditor;
import me.coley.recaf.util.Lang;
import me.coley.recaf.util.Reflect;

/**
 * RefectiveItem decorator for allowing editing of opcode Insn attributes.
 * 
 * @author Matt
 */
public class ReflectiveOpcodeItem extends ReflectiveClassNodeItem {
	private final ClassNode cn;
	private final MethodNode mn;

	public ReflectiveOpcodeItem(InsnListEditor editor, AbstractInsnNode owner, Field field, String categoryKey,
			String translationKey) {
		super(owner, field, categoryKey, translationKey);
		this.cn = editor.getClassNode();
		this.mn = editor.getMethod();
	}

	@Override
	protected Class<?> getEditorType() {
		// check if proper type exists

		if (getType().equals(LabelNode.class)) {
			return LabelEditor.class;
		} else if (getType().equals(Handle.class)) {
			return HandleEditor.class;
		} else if (getType().equals(Object[].class)) {
			return ArgumentEditor.class;
		} else if (getType().equals(Type.class)) {
			return TypeEditor.class;
		} else if (getType().equals(List.class)) {
			java.lang.reflect.Type[] args = getGenericType().getActualTypeArguments();
			if (args != null && args.length > 0) {
				java.lang.reflect.Type t = args[0];
				if (t.equals(Integer.class)) {
					return SwitchKeys.class;
				} else if (t.equals(LabelNode.class)) {
					return SwitchLabels.class;
				}
			}
		}
		{
			// Switch
			// List<String> for keys
			// List<LabelNode> for values
		}
		return null;
	}

	@Override
	public Class<?> getType() {
		// When editing the 'cst' field in the LdcInsnNode, we want to treat it
		// as the descriptor type, not the 'cst' field type (object).
		if (getField().getName().equals("cst")) {
			LdcInsnNode node = (LdcInsnNode) getOwner();
			return node.cst.getClass();
		}
		return super.getType();
	}

	@Override
	public void setValue(Object value) {
		if (checkCaller() && !value.equals(getValue())) {
			super.setValue(value);
			Bus.post(new ClassDirtyEvent(getNode()));
		}
	}

	@Override
	protected ClassNode getNode() {
		return cn;
	}

	/**
	 * Editor for editing keys <i>(in switch opcodes)</i>, as
	 * {@code List<String>}.
	 * 
	 * @author Matt
	 *
	 * @param <T>
	 *            {@code List<String>}
	 */
	public static class SwitchKeys<T extends List<Integer>> extends StagedCustomEditor<T> {
		public SwitchKeys(Item item) {
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
		private void open(SwitchKeys<T> exceptions) {
			if (staged()) {
				return;
			}
			BorderPane listPane = new BorderPane();
			BorderPane menuPane = new BorderPane();
			ListView<Integer> view = new ListView<>();
			ObservableList<Integer> list = FXCollections.observableArrayList(exceptions.getValue());
			list.addListener((ListChangeListener<Integer>) c -> setValue((T) list));
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
			TextField newKey = new TextField();
			newKey.setOnAction((e) -> add(newKey, view));
			Button addInterface = new ActionButton(Lang.get("misc.add"), () -> add(newKey, view));
			menuPane.setCenter(newKey);
			menuPane.setRight(addInterface);
			setStage("ui.bean.opcode.keys.name", listPane, 300, 500);
		}

		/**
		 * Add member in TextField to ListView.
		 * 
		 * @param text
		 * @param view
		 */
		private void add(TextField text, ListView<Integer> view) {
			try {
				int parsed = Integer.parseInt(text.textProperty().get());
				view.itemsProperty().get().add(parsed);
				text.textProperty().setValue("");
			} catch (NumberFormatException e) {}
		}

		/**
		 * Remove selected items from ListView.
		 * 
		 * @param view
		 */
		private void delete(ListView<Integer> view) {
			MultipleSelectionModel<Integer> selection = view.selectionModelProperty().getValue();
			for (int index : selection.getSelectedIndices()) {
				view.getItems().remove(index);
			}
		}
	}

	/**
	 * Editor for editing labels <i>(in switch opcodes)</i>, as
	 * {@code List<LabelNode>}.
	 * 
	 * @author Matt
	 *
	 * @param <T>
	 *            {@code List<LabelNode>}
	 */
	public static class SwitchLabels<T extends List<LabelNode>> extends StagedCustomEditor<T> {
		public SwitchLabels(Item item) {
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
		private void open(SwitchLabels<T> exceptions) {
			if (staged()) {
				return;
			}
			MethodNode method = ((ReflectiveOpcodeItem) item).mn;
			BorderPane listPane = new BorderPane();
			BorderPane menuPane = new BorderPane();
			ListView<LabelNode> view = new ListView<>();
			ObservableList<LabelNode> list = FXCollections.observableArrayList(exceptions.getValue());
			list.addListener(new ListChangeListener<LabelNode>() {
				@SuppressWarnings("unchecked")
				@Override
				public void onChanged(Change<? extends LabelNode> c) {
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
			view.setCellFactory(cell -> new ListCell<LabelNode>() {
				@Override
				protected void updateItem(LabelNode node, boolean empty) {
					super.updateItem(node, empty);
					if (empty || node == null) {
						setGraphic(null);
					} else {
						setGraphic(FormatFactory.opcode(node, method));
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
			LabelButton selectLabel = new LabelButton(Lang.get("ui.bean.opcode.label.nullvalue"), method);
			Button addInterface = new ActionButton(Lang.get("misc.add"), () -> add(selectLabel, view));
			menuPane.setCenter(selectLabel);
			menuPane.setRight(addInterface);
			setStage("ui.bean.opcode.keys.name", listPane, 300, 500);
		}

		/**
		 * Add selected label to ListView.
		 * 
		 * @param lbl
		 * @param view
		 */
		private void add(LabelButton lbl, ListView<LabelNode> view) {
			try {
				view.itemsProperty().get().add(lbl.getLabel());
				lbl.setLabel(null);
			} catch (NumberFormatException e) {}
		}

		/**
		 * Remove selected items from ListView.
		 * 
		 * @param view
		 */
		private void delete(ListView<LabelNode> view) {
			MultipleSelectionModel<LabelNode> selection = view.selectionModelProperty().getValue();
			for (int index : selection.getSelectedIndices()) {
				view.getItems().remove(index);
			}
		}
	}

	/**
	 * Editor for asm Type fields.
	 * 
	 * @author Matt
	 *
	 * @param <T>
	 *            {@code Type}
	 */
	public static class TypeEditor<T extends Type> extends CustomEditor<T> {
		public TypeEditor(Item item) {
			super(item);
		}

		@Override
		public Node getEditor() {
			Type original = (Type) item.getValue();
			return new ReflectiveTextField<Type>(item.getOwner(), item.getField()) {

				protected void setText(Object instance, Field field) {
					this.setText(convert(original));
				}

				@Override
				protected Type convert(String text) {
					return TypeUtil.parse(text);
				}

				@Override
				protected String convert(Type type) {
					return TypeUtil.toString(type);
				}
			};
		}
	}

	/**
	 * Editor for bsmArgs. The code in here is bad. I know that ControlsFX's way
	 * of creating sheets is based off of having context-less conversion of
	 * types based on custom editor nodes, but this IDEALLY would be implemented
	 * with the context being known. Since it cannot be known some ugly code has
	 * been made as a work around.
	 * 
	 * @author Matt
	 */
	public static class ArgumentEditor<T extends Object> extends StagedCustomEditor<Object> {
		public ArgumentEditor(Item item) {
			super(item);
		}

		@Override
		public Node getEditor() {
			Object[] args = (Object[]) getValue();
			if (args[0].getClass() == Type.class && args[2].getClass() == Type.class && args[1].getClass() == Handle.class) {
				return new ActionButton(Lang.get("misc.edit"), () -> open(this));
			} else {
				// Are there other formats? IIRC Stringer-obfuscator does
				// some funky stuff.
				// But even if it were supported, whats the point of looking at
				// garbled data when you can run it through a decrypt tool
				// first?
				// I don't think it'd be worth it.
				return new Label(Lang.get("misc.error"));
			}
		}

		private void open(ArgumentEditor<T> argEditor) {
			if (staged()) {
				return;
			}
			Object[] args = (Object[]) getValue();
			ReflectivePropertySheet psHandle = new ReflectivePropertySheet(args[1]) {
				protected void setupItems(Object instance) {
					// normal
					for (Field field : Reflect.fields(instance.getClass())) {
						if (Access.isStatic(field.getModifiers())) {
							continue;
						}
						field.setAccessible(true);
						String name = field.getName();
						String group = "ui.bean.bsmarg";
						// Setup item & add to list
						if (name.equals("tag")) {
							getItems().add(new ReflectiveItem(instance, field, group, name) {
								@Override
								protected Class<?> getEditorType() {
									return TagEditor.class;
								}
							});
						} else {
							getItems().add(new ReflectiveItem(instance, field, group, name));
						}
					}
				}
			};
			psHandle.setModeSwitcherVisible(false);
			psHandle.setSearchBoxVisible(false);
			ReflectivePropertySheet psType0 = new ReflectivePropertySheet(args[0]) {
				protected void setupItems(Object instance) {
					if (instance instanceof Type) {
						getItems().add(new ReflectiveItem(item.getOwner(), item.getField(), "ui.bean.bsmarg", "type0") {
							@Override
							protected Class<?> getEditorType() {
								return Type0Editor.class;
							}

							@Override
							public Object getValue() {
								return args[0];
							}

							@Override
							public void setValue(Object value) {
								args[0] = value;
							}
						});
					}
				}
			};
			psType0.setModeSwitcherVisible(false);
			psType0.setSearchBoxVisible(false);
			ReflectivePropertySheet psType2 = new ReflectivePropertySheet(args[2]) {
				protected void setupItems(Object instance) {
					if (instance instanceof Type) {
						getItems().add(new ReflectiveItem(item.getOwner(), item.getField(), "ui.bean.bsmarg", "type2") {
							@Override
							protected Class<?> getEditorType() {
								return Type2Editor.class;
							}

							@Override
							public Object getValue() {
								return args[2];
							}

							@Override
							public void setValue(Object value) {
								args[2] = value;
							}
						});
					}
				}
			};
			psType2.setModeSwitcherVisible(false);
			psType2.setSearchBoxVisible(false);
			VBox v = new VBox();
			v.getChildren().add(psHandle);
			v.getChildren().add(psType0);
			v.getChildren().add(psType2);
			setStage("ui.bean.opcode.bsmargs.name", v, 600, 300);
		}

		/**
		 * Editor for bsmArgs[0].
		 * 
		 * @author Matt
		 */
		public static class Type0Editor extends TypeEditor<Type> {
			public Type0Editor(Item item) {
				super(item);
			}

			@Override
			protected int getIndex() {
				return 0;
			}
		}

		/**
		 * Editor for bsmArgs[2].
		 * 
		 * @author Matt
		 */
		public static class Type2Editor extends TypeEditor<Type> {
			public Type2Editor(Item item) {
				super(item);
			}

			@Override
			protected int getIndex() {
				return 2;
			}
		}

		public static abstract class TypeEditor<T extends Type> extends CustomEditor<T> {
			public TypeEditor(Item item) {
				super(item);
			}

			@Override
			public Node getEditor() {
				Object[] array = Reflect.get(item.getOwner(), item.getField());
				Type original = (Type) array[getIndex()];
				return new ReflectiveTextField<Type>(item.getOwner(), item.getField()) {

					protected void setText(Object instance, Field field) {
						this.setText(convert(original));
					}

					@Override
					protected Type convert(String text) {
						// return null by default, in case any parsing errors
						// occur
						Type t = null;
						try {
							// return null if the types do not match the
							// original
							t = Type.getType(text);
							if (t == null || !match(t, original)) {
								return null;
							}
						} catch (Exception e) {}
						return t;
					}

					@Override
					protected String convert(Type text) {
						return text.getDescriptor();
					}

					@Override
					protected void set(Object instance, Field field, Type converted) {
						Array.set(array, getIndex(), converted);
					}

					private boolean match(Type t, Type original) {
						// Pretty sure as long as the kind of type matches
						// (non-methods can be grouped together) it'll be fine.
						return original.getSort() == Type.METHOD && original.getSort() ==  t.getSort();
					}

				};
			}

			protected abstract int getIndex();
		}
	}

	/**
	 * Editor for labels.
	 * 
	 * @author Matt
	 */
	public static class LabelEditor<T extends LabelNode> extends StagedCustomEditor<LabelNode> {
		public LabelEditor(Item item) {
			super(item);
		}

		@Override
		public Node getEditor() {
			MethodNode mn = ((ReflectiveOpcodeItem) item).mn;
			LabelButton btn = new LabelButton(getValue(), mn);
			btn.setUpdateTask(l -> {
				setValue(l);
			});
			return btn;
		}
	}

	/**
	 * Editor for Indy handles.
	 * 
	 * @author Matt
	 */
	public static class HandleEditor<T extends Handle> extends StagedCustomEditor<Handle> {
		public HandleEditor(Item item) {
			super(item);
		}

		@Override
		public Node getEditor() {
			return new ActionButton(Lang.get("misc.edit"), () -> open(this));

		}

		private void open(HandleEditor<T> handleEditor) {
			if (staged()) {
				return;
			}
			Handle h = (Handle) item.getValue();
			ReflectivePropertySheet ps = new ReflectivePropertySheet(h) {
				protected void setupItems(Object instance) {
					for (Field field : Reflect.fields(instance.getClass())) {
						field.setAccessible(true);
						String name = field.getName();
						String group = "ui.bean.handle";
						// Setup item & add to list
						if (name.equals("tag")) {
							getItems().add(new ReflectiveItem(instance, field, group, name) {
								@Override
								protected Class<?> getEditorType() {
									return TagEditor.class;
								}
							});
						} else {
							getItems().add(new ReflectiveItem(instance, field, group, name));
						}
					}
				}
			};
			ps.setModeSwitcherVisible(false);
			ps.setSearchBoxVisible(false);
			setStage("ui.bean.opcode.bsm.name", ps, 300, 300);
		}
	}

	/**
	 * Editor for Handle tags.
	 * 
	 * @author Matt
	 *
	 * @param <T>
	 */
	public static class TagEditor<T extends Integer> extends CustomEditor<T> {
		public TagEditor(Item item) {
			super(item);
		}

		@Override
		public Node getEditor() {
			//@formatter:off
			List<Integer> opts = Arrays.asList(
					Opcodes.H_GETFIELD,
					Opcodes.H_GETSTATIC, 
					Opcodes.H_PUTFIELD,
					Opcodes.H_PUTSTATIC, 
					Opcodes.H_INVOKEINTERFACE, 
					Opcodes.H_INVOKESPECIAL,
					Opcodes.H_INVOKESTATIC,
					Opcodes.H_INVOKEVIRTUAL);
			List<String> optStrs = Arrays.asList(
					"H_GETFIELD", 
					"H_GETSTATIC", 
					"H_PUTFIELD", 
					"H_PUTSTATIC",
					"H_INVOKEINTERFACE", 
					"H_INVOKESPECIAL", 
					"H_INVOKESTATIC", 
					"H_INVOKEVIRTUAL");
			return new ReflectiveCombo<>(item.getOwner(), item.getField(), opts, optStrs);
			//@formatter:on
		}
	}

}
