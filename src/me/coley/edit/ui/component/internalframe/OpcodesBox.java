package me.coley.edit.ui.component.internalframe;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.edit.Program;
import me.coley.edit.ui.component.list.OpcodeCellRenderer;

@SuppressWarnings("serial")
public class OpcodesBox extends BasicFrame {
	public OpcodesBox(Program callback, MethodNode mn) throws Exception {
		super("Opcodes: " + mn.name);
		setMaximumSize(new Dimension(1000, 1000));

		setLayout(new BorderLayout());
		// Opcodes list
		DefaultListModel<AbstractInsnNode> model = new DefaultListModel<>();
		JList<AbstractInsnNode> opcodes = new JList<>();
		opcodes.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		for (AbstractInsnNode ain : mn.instructions.toArray()) {
			model.addElement(ain);
		}
		opcodes.setModel(model);
		opcodes.setCellRenderer(new OpcodeCellRenderer(mn, callback.options));
		add(new JScrollPane(opcodes), BorderLayout.CENTER);
		setVisible(true);
	}
}
