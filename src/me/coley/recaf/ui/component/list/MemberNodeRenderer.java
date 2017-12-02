package me.coley.recaf.ui.component.list;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.asm.Access;
import me.coley.recaf.config.UiConfig;
import me.coley.recaf.ui.Icons;

import static me.coley.recaf.ui.Icons.*;

/**
 * Member node renderer.
 *
 * @author Matt
 */
public class MemberNodeRenderer implements RenderFormatter<Object> {
	private final UiConfig options;

	public MemberNodeRenderer(UiConfig options) {
		this.options = options;
	}

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		formatList(list);
		String display = value.toString();
		int access = -1;
		boolean method = false;
		if (value instanceof MethodNode) {
			MethodNode node = (MethodNode) value;
			display = getMethodText(node);
			access = node.access;
			method = true;
		} else if (value instanceof FieldNode) {
			FieldNode node = (FieldNode) value;
			display = getFieldText(node);
			access = node.access;
		}
		JLabel label = new JLabel("<html>" + display + "</html>");
		formatLabel(label, isSelected);
		JPanel content = new JPanel();
		content.setBackground(label.getBackground());
		content.setLayout(new BoxLayout(content, BoxLayout.X_AXIS));
		content.add(label);
		addAccess(content, access, method, isSelected);
		return content;
	}

	private void addAccess(JPanel content, int access, boolean method, boolean selected) {
		if (Access.isPublic(access)) {
			addIcon(content, selected, method ? Icons.ML_PUBLIC : Icons.FL_PUBLIC);
		} else if (Access.isProtected(access)) {
			addIcon(content, selected, method ? Icons.ML_PROTECTED : Icons.FL_PROTECTED);
		} else if (Access.isPrivate(access)) {
			addIcon(content, selected, method ? Icons.ML_PRIVATE : Icons.FL_PRIVATE);
		} else if (Access.isPrivate(access)) {
			addIcon(content, selected, method ? Icons.ML_PRIVATE : Icons.FL_PRIVATE);
		} else {
			addIcon(content, selected, method ? Icons.ML_DEFAULT : Icons.FL_DEFAULT);
		}
		if (Access.isAbstract(access)) {
			addIcon(content, selected, MOD_ABSTRACT);
		}
		if (Access.isFinal(access)) {
			addIcon(content, selected, MOD_FINAL);
		}
		if (Access.isNative(access)) {
			addIcon(content, selected, MOD_NATIVE);
		}
		if (Access.isStatic(access)) {
			addIcon(content, selected, MOD_STATIC);
		}
		if (Access.isTransient(access)) {
			addIcon(content, selected, MOD_TRANSIENT);
		}
		if (Access.isVolatile(access)) {
			addIcon(content, selected, MOD_VOLATILE);
		}
		if (Access.isSynthetic(access) || Access.isBridge(access)) {
			addIcon(content, selected, MOD_SYNTHETIC);
		}
	}

	private void addIcon(JPanel content, boolean selected, Icon icon) {
		JLabel lbl = new JLabel(icon);
		lbl.setPreferredSize(new Dimension(18, 16));
		formatLabel(lbl, selected);
		content.add(lbl);
	}

	private String getFieldText(FieldNode node) {
		return italic(color(getTheme().memberReturnType, getTypeStr(Type.getType(node.desc), options))) + " " + color(
				getTheme().memberName, escape(node.name));
	}

	private String getMethodText(MethodNode node) {
		Type typeMethod = Type.getMethodType(node.desc);
		// Args string
		String args = "";
		for (Type t : typeMethod.getArgumentTypes()) {
			args += getTypeStr(t, options) + ", ";
		}
		if (args.endsWith(", ")) {
			args = args.substring(0, args.length() - 2);
		}
		String s = italic(color(getTheme().memberReturnType, getTypeStr(typeMethod.getReturnType(), options))) + " ";
		s += color(getTheme().memberName, escape(node.name)) + "(";
		s += color(getTheme().memberParameterType, args);
		s += ")";
		return s;
	}
}
