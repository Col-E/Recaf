package me.coley.recaf.ui.component;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import org.objectweb.asm.tree.*;

import me.coley.recaf.ui.component.editor.*;
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
				return AccessFieldEditor.class;
			} else if (getField().getName().equals("name")) {
				return FieldNameEditor.class;
			}
			String desc = ((FieldNode) getOwner()).desc;
			if (getField().getName().equals("value") && Misc.getTypeClass(desc) != null) {
				return DefaultValueEditor.class;
			}
			return null;
		}
		// check raw-type for list
		if (!type.getRawType().equals(List.class)) {
			return null;
		}
		// Create custom editor for different argument types.
		Type arg = type.getActualTypeArguments()[0];
		if (arg.equals(AnnotationNode.class)) {
			// annotation lists
			return AnnotationListEditor.class;
		} else if (arg.equals(TypeAnnotationNode.class)) {
			// type-annotation lists
			return AnnotationTypeListEditor.class;
		}
		return null;
	}

	@Override
	public Class<?> getType() {
		// When editing the 'value' field in the FieldNode, we want to treat it
		// as the descriptor type, not the 'value' field type (object).
		if (getField().getName().equals("value")) {
			FieldNode node = (FieldNode) getOwner();
			Class<?> type = Misc.getTypeClass(node.desc);
			if (type != null) {
				return type;
			}
		}
		return super.getType();
	}

	@Override
	public ClassNode getNode() {
		return fieldOwner;
	}
}
