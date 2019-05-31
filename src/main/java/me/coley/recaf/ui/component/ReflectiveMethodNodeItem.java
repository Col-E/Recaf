package me.coley.recaf.ui.component;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import org.objectweb.asm.tree.*;
import me.coley.recaf.ui.component.editor.*;

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
				return AccessMethodEditor.class;
			} else if (getField().getName().equals("instructions")) {
				return InsnProxyListEditor.class;
			} else if (getField().getName().equals("name")) {
				return MethodNameEditor.class;
			}
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
			return ExceptionListEditor.class;
		} else if (arg.equals(TryCatchBlockNode.class)) {
			// exceptions
			return TryCatchListEditor.class;
		} else if (arg.equals(LocalVariableNode.class)) {
			// variables
			return LocalVarListEditor.class;
		} else if (arg.equals(ParameterNode.class)) {
			// parameters
			return ParameterListEditor.class;
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
	public ClassNode getNode() {
		return methodOwner;
	}

	public MethodNode getMethodNode() {
		return method;
	}
}
