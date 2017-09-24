package me.coley.recaf.ui.component.internalframe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import me.coley.recaf.ui.component.LabeledComponent;
import me.coley.recaf.ui.component.action.ActionButton;
import me.coley.recaf.ui.component.action.ActionTextField;

@SuppressWarnings("serial")
public class TryCatchBox extends BasicFrame {
	private static final Color bg = new Color(166, 166, 166);
	private final JScrollPane scroll = new JScrollPane();

	public TryCatchBox(MethodNode mn) {
		super("Exceptions: " + mn.name);
		scroll.setMaximumSize(new Dimension(400, 620));
		setMaximumSize(new Dimension(400, 600));
		setBackground(bg);
		setLayout(new BorderLayout());
		add(scroll, BorderLayout.CENTER);
		update(mn);
		setVisible(true);
	}

	private void update(MethodNode mn) {
		// scroll.removeAll();
		JPanel a = new JPanel();
		a.setLayout(new GridLayout(0, 1));
		for (int i = 0; i < mn.tryCatchBlocks.size(); i++) {
			final int j = i;
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			panel.setBorder(BorderFactory.createEtchedBorder());
			TryCatchBlockNode block = mn.tryCatchBlocks.get(i);
			List<JComponent> comps = new ArrayList<>();
			comps.add(new JLabel("<html><b>Start</b>: " + mn.instructions.indexOf(block.start) + "</html>"));
			comps.add(new JLabel("<html><b>End</b>: " + mn.instructions.indexOf(block.end) + "</html>"));
			comps.add(new JLabel("<html><b>Handler</b>: " + mn.instructions.indexOf(block.handler) + "</html>"));
			comps.add(new LabeledComponent("<html><b>Type</b>: ", new ActionTextField(block.type, s -> block.type = s)));
			comps.add(new ActionButton("Remove", () -> {
				mn.tryCatchBlocks.remove(j);
				update(mn);
			}));
			for (JComponent c : comps) {
				c.setAlignmentX(JComponent.LEFT_ALIGNMENT);
				panel.add(c);
			}
			a.add(panel);
		}
		scroll.setViewportView(a);
		// scroll.repaint();
		// scroll.validate();
	}
}
