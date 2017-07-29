package me.coley.recaf.ui.component.internalframe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JScrollPane;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.Program;
import me.coley.recaf.ui.component.ClassDisplayPanel;
import me.coley.recaf.ui.component.list.OpcodeList;

@SuppressWarnings("serial")
public class OpcodesBox extends BasicFrame {
	private static final Color bg = new Color(166, 166, 166);
	public OpcodesBox(Program callback,ClassDisplayPanel display, MethodNode mn) throws Exception {
		super("Opcodes: " + mn.name);
		setMaximumSize(new Dimension(1000, 1000));
		setBackground(bg);
		setLayout(new BorderLayout());
		// Opcodes list
		add(new JScrollPane(new OpcodeList(callback, display,mn)), BorderLayout.CENTER);
		setVisible(true);
	}
}
