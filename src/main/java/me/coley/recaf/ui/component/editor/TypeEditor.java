package me.coley.recaf.ui.component.editor;

import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.*;
import javafx.scene.Node;
import me.coley.recaf.bytecode.TypeUtil;
import me.coley.recaf.ui.component.*;
import me.coley.recaf.ui.component.ReflectivePropertySheet.CustomEditor;

/**
 * Editor for asm Type fields.
 * 
 * @author Matt
 */
public class TypeEditor extends CustomEditor<Type> {
	public TypeEditor(Item item) {
		super(item);
	}

	@Override
	public Node getEditor() {
		return new PropertyTextField<Type>(TypeEditor.this) {
			@Override
			protected Type convertTextToValue(String text) {
				return TypeUtil.parse(text);
			}

			@Override
			protected String convertValueToText(Type type) {
				return TypeUtil.toString(type);
			}
		};
	}
}