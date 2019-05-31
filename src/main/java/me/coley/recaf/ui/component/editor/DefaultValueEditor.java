package me.coley.recaf.ui.component.editor;

import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.property.editor.Editors;
import org.controlsfx.property.editor.PropertyEditor;
import org.objectweb.asm.tree.*;
import javafx.scene.Node;
import me.coley.recaf.ui.FormatFactory;
import me.coley.recaf.util.Misc;

/**
 * Editor for editing field's default-value.
 * 
 * @author Matt
 */
public class DefaultValueEditor<T> extends StagedCustomEditor<T> {
	public DefaultValueEditor(Item item) {
		super(item);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Node getEditor() {
		FieldNode field = (FieldNode) item.getOwner();
		Class<?> valueClass = Misc.getTypeClass(field.desc);
		PropertyEditor<T> editor = null;
		if (String.class.equals(valueClass)) {
			editor = (PropertyEditor<T>) Editors.createTextEditor(item);
		} else if (Misc.isNumeric(valueClass)) {
			editor = (PropertyEditor<T>) Editors.createNumericEditor(item);
		} else {
			// Shouldn't happen unless java/ASM update to allow
			// defaultValue's for other types
			return FormatFactory.name("Unknown type: " + valueClass.getName());
		}
		T value = getValue();
		if (value != null) editor.setValue(value);
		return editor.getEditor();
	}
}