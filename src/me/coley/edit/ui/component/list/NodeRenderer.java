package me.coley.edit.ui.component.list;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.edit.ui.FontUtil;

public class NodeRenderer implements ListCellRenderer<Object> {
	private static final Color bg = new Color(200,200,200);
	private static final Color bg2 = new Color(166,166,166);
	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		list.setBackground(bg2);
		String display = value.toString();
		if (value instanceof MethodNode) {
			MethodNode node = (MethodNode) value;
			display = node.name + node.desc;
		} else if (value instanceof FieldNode) {
			FieldNode node = (FieldNode) value;
			display = node.name + " " + node.desc;
		}
		JLabel label = new JLabel(display);
		label.setFont(FontUtil.monospace);
		label.setOpaque(true);
		label.setBorder(BorderFactory.createEtchedBorder());
		if (isSelected) {
			label.setBackground(Color.white);
		} else {
			label.setBackground(bg);
		}
		return label;
	}

}
