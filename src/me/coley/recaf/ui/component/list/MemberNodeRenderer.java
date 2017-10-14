package me.coley.recaf.ui.component.list;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.Options;
import me.coley.recaf.asm.Access;
import me.coley.recaf.ui.FontUtil;
import me.coley.recaf.ui.HtmlRenderer;
import me.coley.recaf.ui.Icons;

import static me.coley.recaf.ui.Icons.*;

/**
 * Member node renderer.
 *
 * @author Matt
 */
public class MemberNodeRenderer implements HtmlRenderer, ListCellRenderer<Object> {
	private static final Color bg = new Color(200, 200, 200);
	private static final Color bg2 = new Color(166, 166, 166);
	private final Options options;

	public MemberNodeRenderer(Options options) {
		this.options = options;
	}

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		list.setBackground(bg2);
		String display = value.toString();
		int access = -1;
		boolean method = false;
		if (value instanceof MethodNode) {
			MethodNode node = (MethodNode) value;
			display = formatMethod(node);
			access = node.access;
			method = true;
		} else if (value instanceof FieldNode) {
			FieldNode node = (FieldNode) value;
			display = formatField(node);
			access = node.access;
		}
		JPanel content = new JPanel();
		content.setBackground(bg2);
		content.setLayout(new BoxLayout(content, BoxLayout.X_AXIS));
		JLabel label = new JLabel("<html>" + display + "</html>");
		label.setFont(FontUtil.monospace);
		formatComponent(label, isSelected);
		content.add(label);
		addAccess(content, access, method, isSelected);
		return content;
	}

	private void addAccess(JPanel content, int access, boolean method, boolean selected) {
		if (Access.isPublic(access)) {
			add(content, selected, method ? Icons.ML_PUBLIC : Icons.FL_PUBLIC);
		} else if (Access.isProtected(access)) {
			add(content, selected, method ? Icons.ML_PROTECTED : Icons.FL_PROTECTED);
		} else if (Access.isPrivate(access)) {
			add(content, selected, method ? Icons.ML_PRIVATE : Icons.FL_PRIVATE);
		} else if (Access.isPrivate(access)) {
			add(content, selected, method ? Icons.ML_PRIVATE : Icons.FL_PRIVATE);
		} else {
			add(content, selected, method ? Icons.ML_DEFAULT : Icons.FL_DEFAULT);
		}
		if (Access.isAbstract(access)) {
			add(content, selected, MOD_ABSTRACT);
		}
		if (Access.isFinal(access)) {
			add(content, selected, MOD_FINAL);
		}
		if (Access.isNative(access)) {
			add(content, selected, MOD_NATIVE);
		}
		if (Access.isStatic(access)) {
			add(content, selected, MOD_STATIC);
		}
		if (Access.isTransient(access)) {
			add(content, selected, MOD_TRANSIENT);
		}
		if (Access.isVolatile(access)) {
			add(content, selected, MOD_VOLATILE);
		}
		if (Access.isSynthetic(access) || Access.isBridge(access)) {
			add(content, selected, MOD_SYNTHETIC);
		}
	}

	private void add(JPanel content, boolean selected, Icon icon) {
		JLabel lbl = new JLabel(icon);
		lbl.setPreferredSize(new Dimension(18,16));
		formatComponent(lbl, selected);
		content.add(lbl);
	}

	private void formatComponent(JComponent component, boolean selected) {
		component.setOpaque(true);
		component.setBorder(BorderFactory.createEtchedBorder());
		if (selected) {
			component.setBackground(Color.white);
		} else {
			component.setBackground(bg);
		}
	}

	private String formatField(FieldNode node) {
		return italic(color(colBlueDark, getTypeStr(Type.getType(node.desc), options))) + " " + escape(node.name);
	}

	private String formatMethod(MethodNode node) {
		Type typeMethod = Type.getMethodType(node.desc);
		// Args string
		String args = "";
		for (Type t : typeMethod.getArgumentTypes()) {
			args += getTypeStr(t, options) + ", ";
		}
		if (args.endsWith(", ")) {
			args = args.substring(0, args.length() - 2);
		}
		String s = italic(color(colBlueDark, getTypeStr(typeMethod.getReturnType(), options))) + " ";
		s += color(colRedDark, escape(node.name)) + "(";
		s += color(colTealDark, args);
		s += ")";
		return s;
	}
}
