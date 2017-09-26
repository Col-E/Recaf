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
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import me.coley.recaf.ui.component.LabeledComponent;
import me.coley.recaf.ui.component.action.ActionButton;
import me.coley.recaf.ui.component.action.ActionTextField;
import me.coley.recaf.ui.component.panel.LabelSwitcherPanel;

@SuppressWarnings("serial")
public class TryCatchBox extends BasicFrame {
	private static final Color bg = new Color(166, 166, 166);
	private final JScrollPane scroll = new JScrollPane();

	public TryCatchBox(MethodNode mn) {
		super("Exceptions: " + mn.name);
		setBackground(bg);
		setLayout(new BorderLayout());
		add(scroll, BorderLayout.CENTER);
		update(mn);
		int k = 162;
		int s = Math.min(k*2, k * mn.tryCatchBlocks.size());
		scroll.setPreferredSize(new Dimension(350, s));
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
			comps.add(new LabeledComponent("<html><b>Start</b>: ", new LabelSwitcherPanel( mn, block.start, l ->  block.start = l)));
			comps.add(new LabeledComponent("<html><b>End</b>: ", new LabelSwitcherPanel( mn, block.end, l ->  block.end = l)));
			comps.add(new LabeledComponent("<html><b>Handler</b>: ", new LabelSwitcherPanel( mn, block.handler, l ->  block.handler = l)));
			comps.add(new LabeledComponent("<html><b>Start</b>: ", new LabelSwitcherPanel( mn, block.start, l ->  block.start = l)));
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
