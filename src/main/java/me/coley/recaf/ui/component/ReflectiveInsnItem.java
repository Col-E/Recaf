package me.coley.recaf.ui.component;

import java.lang.reflect.Field;
import java.util.List;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import me.coley.event.Bus;
import me.coley.recaf.event.ClassDirtyEvent;
import me.coley.recaf.ui.component.editor.*;

/**
 * RefectiveItem decorator for allowing editing of instruction attributes.
 * 
 * @author Matt
 */
public class ReflectiveInsnItem extends ReflectiveClassNodeItem {
	private final ClassNode cn;
	private final MethodNode mn;

	public ReflectiveInsnItem(InsnListEditor editor, AbstractInsnNode owner, Field field, String categoryKey,
							  String translationKey) {
		super(owner, field, categoryKey, translationKey);
		this.cn = editor.getClassNode();
		this.mn = editor.getMethod();
	}

	@Override
	protected Class<?> getEditorType() {
		// check if proper type exists
		if (isLdcCST() && getType().equals(String.class)) {
			return StringEditor.class;
		} else if (getType().equals(LabelNode.class)) {
			return LabelEditor.Opcode.class;
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
					return SwitchKeyListEditor.class;
				} else if (t.equals(LabelNode.class)) {
					return SwitchLabels.class;
				}
			}
		}
		return null;
	}

	@Override
	public Class<?> getType() {
		// When editing the 'cst' field in the LdcInsnNode, we want to treat it
		// as the descriptor type, not the 'cst' field type (Object).
		if (isLdcCST()) {
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
	public ClassNode getNode() {
		return cn;
	}

	public MethodNode getMethod() {
		return mn;
	}

	private boolean isLdcCST() {
		return getField().getName().equals("cst");
	}
}
