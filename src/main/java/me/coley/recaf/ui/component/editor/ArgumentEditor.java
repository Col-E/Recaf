package me.coley.recaf.ui.component.editor;

import java.lang.reflect.Field;
import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import me.coley.recaf.bytecode.AccessFlag;
import me.coley.recaf.ui.component.*;
import me.coley.recaf.util.Lang;
import me.coley.recaf.util.Reflect;

/**
 * Editor for bsmArgs. The code in here is bad. I know that ControlsFX's way of
 * creating sheets is based off of having context-less conversion of types based
 * on custom editor nodes, but this IDEALLY would be implemented with the
 * context being known. Since it cannot be known some ugly code has been made as
 * a work around.
 * 
 * @author Matt
 */
public class ArgumentEditor extends StagedCustomEditor<Object> {
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

	private void open(ArgumentEditor argEditor) {
		if (staged()) {
			return;
		}
		Object[] args = (Object[]) getValue();
		ReflectivePropertySheet psHandle = new ReflectivePropertySheet(args[1]) {
			protected void setupItems(Object instance) {
				// normal
				for (Field field : Reflect.fields(instance.getClass())) {
					if (AccessFlag.isStatic(field.getModifiers())) {
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
	public static class Type0Editor extends TypeFromIndyEditor {
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
	public static class Type2Editor extends TypeFromIndyEditor {
		public Type2Editor(Item item) {
			super(item);
		}

		@Override
		protected int getIndex() {
			return 2;
		}
	}
}