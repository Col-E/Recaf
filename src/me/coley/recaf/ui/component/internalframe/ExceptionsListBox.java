package me.coley.recaf.ui.component.internalframe;

import java.awt.Color;
import java.awt.GridLayout;

import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.ui.component.action.ActionTextField;

@SuppressWarnings("serial")
public class ExceptionsListBox extends BasicFrame {
	private static final Color bg = new Color(166, 166, 166);

	public ExceptionsListBox(MethodNode mn) {
		super("Exceptions: " + mn.name);
		setBackground(bg);
		this.setLayout(new GridLayout(mn.exceptions.size(), 0));
		for (int i = 0; i < mn.exceptions.size(); i++) {
			final int j = i;
			String ex = mn.exceptions.get(i);
			add(new ActionTextField(ex, s -> mn.exceptions.set(j, s)));
		}
		setVisible(true);
	}
}
