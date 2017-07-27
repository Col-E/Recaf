package me.coley.edit.ui.component.internalframe;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.DefaultListModel;
import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.edit.ui.component.list.OpcodeCellRenderer;

@SuppressWarnings("serial")
public class OpcodesBox extends JInternalFrame {
	public OpcodesBox(MethodNode mn) throws Exception {
		super("Opcodes: " + mn.name);
		int padding = 12;
		setMaximumSize(new Dimension(300, 300));
		setResizable(true);
		setIconifiable(true);
		setClosable(true);
		setVisible(true);
		setLayout(new BorderLayout());
		// Opcodes list
		DefaultListModel<AbstractInsnNode> model = new DefaultListModel<>();
		JList<AbstractInsnNode> opcodes = new JList<>();
		opcodes.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		for (AbstractInsnNode ain : mn.instructions.toArray()) {
			model.addElement(ain);
		}
		opcodes.setModel(model);
		opcodes.setCellRenderer(new OpcodeCellRenderer());
		add(new JScrollPane(opcodes), BorderLayout.CENTER);
		pack();
		setSize(getWidth() + padding, getHeight() + padding);
	}
}
