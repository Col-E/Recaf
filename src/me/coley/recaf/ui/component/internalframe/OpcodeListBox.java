package me.coley.recaf.ui.component.internalframe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JScrollPane;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.ui.component.list.OpcodeList;
import me.coley.recaf.ui.component.panel.ClassDisplayPanel;

@SuppressWarnings("serial")
public class OpcodeListBox extends BasicFrame {
	private static final Color bg = new Color(166, 166, 166);
	public final OpcodeList list;

	public OpcodeListBox(ClassDisplayPanel display, MethodNode mn) throws Exception {
		super("Opcodes: " + mn.name);
		setBackground(bg);
		setLayout(new BorderLayout());
		setMaximumSize(new Dimension(900, 900));
		// Opcodes list
		add(new JScrollPane(list = new OpcodeList(display, mn)), BorderLayout.CENTER);
		setVisible(true);
	}
}