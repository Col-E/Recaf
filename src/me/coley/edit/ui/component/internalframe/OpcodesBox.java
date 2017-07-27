package me.coley.edit.ui.component.internalframe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Consumer;

import javax.swing.JInternalFrame;

import org.objectweb.asm.tree.MethodNode;

import me.coley.edit.ui.component.LabeledComponent;
import me.coley.edit.ui.component.action.ActionTextField;

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
		
		pack();
		setSize(getWidth() + padding, getHeight() + padding);
	}
}
