package me.coley.recaf.ui.component.editor;

import java.lang.reflect.Field;

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