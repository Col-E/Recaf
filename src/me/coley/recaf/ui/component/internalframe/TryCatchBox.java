package me.coley.recaf.ui.component.internalframe;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import me.coley.recaf.ui.component.LabeledComponent;
import me.coley.recaf.ui.component.action.ActionButton;
import me.coley.recaf.ui.component.action.ActionTextField;

@SuppressWarnings("serial")
public class TryCatchBox extends BasicFrame {
	private static final Color bg = new Color(166, 166, 166);

	public TryCatchBox(MethodNode mn) {
		super("Exceptions: " + mn.name);
		setMaximumSize(new Dimension(1000, 1000));
		setBackground(bg);
		setLayout(new GridLayout(mn.tryCatchBlocks.size(), 0));
		update(mn);
	
		setVisible(true);
	}
	
	private void update(MethodNode mn) {
		getContentPane().removeAll();
		for (int i = 0; i < mn.tryCatchBlocks.size(); i++) {
			final int j = i;
			JPanel panel = new JPanel();
			panel.setLayout(new GridLayout(5, 0));
			panel.setBorder(BorderFactory.createEtchedBorder());
			TryCatchBlockNode block = mn.tryCatchBlocks.get(i);
			panel.add(new JLabel("<html><b>Start</b>: " + mn.instructions.indexOf(block.start) + "</html>"));
			panel.add(new JLabel("<html><b>End</b>: " + mn.instructions.indexOf(block.end) + "</html>"));
			panel.add(new JLabel("<html><b>Handler</b>: " + mn.instructions.indexOf(block.handler) + "</html>"));
			panel.add(new LabeledComponent("<html><b>Type</b>: ", new ActionTextField(block.type, s -> block.type = s)));
			panel.add(new ActionButton("Remove", () -> {
				mn.tryCatchBlocks.remove(j);
				update(mn);
			}));
			add(panel);
		}
		getContentPane().repaint();
		getContentPane().validate();
	}
}
