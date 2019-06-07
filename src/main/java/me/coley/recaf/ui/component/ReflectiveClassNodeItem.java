package me.coley.recaf.ui.component;

import me.coley.event.Bus;
import me.coley.recaf.event.ClassDirtyEvent;
import me.coley.recaf.ui.component.ReflectivePropertySheet.ReflectiveItem;
import me.coley.recaf.ui.component.editor.*;
import org.objectweb.asm.tree.*;
import java.lang.reflect.*;
import java.util.List;

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
				return AccessClassEditor.class;
			} else if (getField().getName().equals("version")) {
				return VersionEditor.class;
			} else if (getField().getName().equals("name")) {
				return ClassNameEditor.class;
			} else if (getField().getName().equals("superName")) {
				return ClassSuperNameEditor.class;
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
			return InterfaceListEditor.class;
		} else if (arg.equals(InnerClassNode.class)) {
			// inner classes
			return InnerClassListEditor.class;
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
	public ClassNode getNode() {
		return (ClassNode) getOwner();
	}
}
