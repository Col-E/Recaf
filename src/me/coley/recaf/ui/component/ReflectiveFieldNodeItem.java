package me.coley.recaf.ui.component;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.property.editor.Editors;
import org.controlsfx.property.editor.PropertyEditor;
import org.objectweb.asm.tree.*;

import javafx.scene.Node;
import javafx.scene.control.TextField;
import me.coley.event.Bus;
import me.coley.recaf.event.FieldRenameEvent;
import me.coley.recaf.ui.FormatFactory;
import me.coley.recaf.ui.component.AccessButton.AccessContext;
import me.coley.recaf.util.Misc;

/**
 * RefectiveItem decorator for allowing editing of FieldNode attributes.
 * 
 * @author Matt
 */
public class ReflectiveFieldNodeItem extends ReflectiveClassNodeItem {
	private final ClassNode fieldOwner;

	public ReflectiveFieldNodeItem(ClassNode fieldOwner, FieldNode owner, Field field, String categoryKey,
			String translationKey) {
		super(owner, field, categoryKey, translationKey);
		this.fieldOwner = fieldOwner;
	}

	@Override
	protected Class<?> getEditorType() {
		// check if proper type exists
		ParameterizedType type = getGenericType();
		if (type == null || type.getRawType() == null) {
			// custom editor for access / version
			if (getField().getName().equals("access")) {
				return AccessEditor.class;
			} else if (getField().getName().equals("name")) {
				return FieldNameEditor.class;
			}
			String desc = ((FieldNode) getOwner()).desc;
			if (getField().getName().equals("value") && Misc.getType(desc) != null) {
				return ValueEditor.class;
			}
			return null;
		}
		// check raw-type for list
		if (!type.getRawType().equals(List.class)) {
			return null;
		}
		// Create custom editor for different argument types.
		Type arg = type.getActualTypeArguments()[0];
		if (arg.equals(AnnotationNode.class) || arg.equals(TypeAnnotationNode.class)) {
			// annotations will eventually go here
			return AnnotationListEditor.class;
		}
		return null;
	}

	@Override
	public Class<?> getType() {
		// When editing the 'value' field in the FieldNode, we want to treat it
		// as the descriptor type, not the 'value' field type (object).
		if (getField().getName().equals("value")) {
			FieldNode node = (FieldNode) getOwner();
			Class<?> type = Misc.getType(node.desc);
			if (type != null) {
				return type;
			}
		}
		return super.getType();
	}

	@Override
	protected ClassNode getNode() {
		return fieldOwner;
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
			return new AccessButton(AccessContext.FIELD, getValue().intValue()) {
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
	public static class FieldNameEditor<T extends String> extends StagedCustomEditor<T> {
		public FieldNameEditor(Item item) {
			super(item);
		}

		@Override
		public Node getEditor() {
			ReflectiveFieldNodeItem refItem = (ReflectiveFieldNodeItem) item;
			ClassNode cn = refItem.getNode();
			FieldNode fn = (FieldNode) refItem.getOwner();
			TextField txtName = new TextField();
			txtName.setText(fn.name);
			txtName.setOnAction(e -> rename(cn, fn, txtName));
			return txtName;
		}

		private void rename(ClassNode owner, FieldNode field, TextField txtName) {
			String text = txtName.getText();
			if (!txtName.isDisabled() && !text.equals(field.name)) {
				Bus.post(new FieldRenameEvent(owner, field, field.name, text));
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
	public static class ValueEditor<T> extends StagedCustomEditor<T> {
		public ValueEditor(Item item) {
			super(item);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Node getEditor() {
			FieldNode field = (FieldNode) item.getOwner();
			Class<?> valueClass = Misc.getType(field.desc);
			PropertyEditor<T> editor = null;
			if (valueClass.equals(String.class)) {
				editor = (PropertyEditor<T>) Editors.createTextEditor(item);
			} else if (Misc.isNumeric(valueClass)) {
				editor = (PropertyEditor<T>) Editors.createNumericEditor(item);
			} else {
				// Shouldn't happen unless java/ASM update to allow
				// defaultValue's for other types
				return FormatFactory.name("Unknown type: " + valueClass.getName());
			}
			editor.setValue(getValue());
			return editor.getEditor();
		}
	}
}
