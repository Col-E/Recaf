package me.coley.recaf.ui.component.internalframe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.JPanel;

import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.ui.component.action.ActionButton;
import me.coley.recaf.ui.component.action.ActionTextField;

@SuppressWarnings("serial")
public class ExceptionsListBox extends BasicFrame {
	private static final Color bg = new Color(166, 166, 166);

	public ExceptionsListBox(MethodNode mn) {
		super("Exceptions: " + mn.name);
		setBackground(bg);
		setLayout(new GridLayout(mn.exceptions.size(), 0));
		update(mn);
		setVisible(true);
	}

	private void update(MethodNode mn) {
		getContentPane().removeAll();
		for (int i = 0; i < mn.exceptions.size(); i++) {
			final int j = i;
			String ex = mn.exceptions.get(i);
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			panel.add(new ActionButton("Delete", () -> {
				mn.exceptions.remove(j);
				update(mn);
			}), BorderLayout.WEST);
			panel.add(new ActionTextField(ex, s -> mn.exceptions.set(j, s)), BorderLayout.CENTER);
			add(panel);
		}
		getContentPane().repaint();
		getContentPane().validate();
	}
}
