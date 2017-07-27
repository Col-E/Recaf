package me.coley.edit.ui.component.list;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.objectweb.asm.tree.AbstractInsnNode;

import me.coley.edit.asm.OpcodeUtil;

public class OpcodeCellRenderer implements ListCellRenderer<AbstractInsnNode> {
	private static final Color bg = new Color(200,200,200);
	private static final Color bg2 = new Color(166,166,166);
	@Override
	public Component getListCellRendererComponent(JList<? extends AbstractInsnNode> list, AbstractInsnNode value, int index,
			boolean isSelected, boolean cellHasFocus) {
		list.setBackground(bg2);
		String display = OpcodeUtil.opcodeToName(value.getOpcode());
		 
		JLabel label = new JLabel(display);
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
