package me.coley.recaf.ui.component.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * JPanel for updating label values.
 * 
 * @author Matt
 */
@SuppressWarnings("serial")
public class LabelSwitcherPanel extends JPanel implements Opcodes {
	/**
	 * Map of radio buttons to possible opcodes.
	 */
	private final Map<JRadioButton, LabelNode> labels = new LinkedHashMap<>();
	/**
	 * Initial label value.
	 */
	private final LabelNode initial;
	/**
	 * Callback to the label setter.
	 */
	private final Consumer<LabelNode> updater;
	/**
	 * Reference so list can be re-painted.
	 */
	private final JList<AbstractInsnNode> list;
	/**
	 * Content wrapper.
	 */
	private final JPanel content = new JPanel();

	public LabelSwitcherPanel(JList<AbstractInsnNode> list, MethodNode method, LabelNode initial, Consumer<LabelNode> updater) {
		this.initial = initial;
		this.updater = updater;
		this.list = list;
		setLayout(new BorderLayout());
		JScrollPane scroll = new JScrollPane(content);
		populate(method.instructions);
		int height = Math.min(200, labels.size() * 20);
		scroll.setPreferredSize(new Dimension(300, height));
		add(scroll, BorderLayout.CENTER);
	}

	private void populate(InsnList opcodes) {
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		for (int i = 0; i < opcodes.size(); i++) {
			AbstractInsnNode ain = opcodes.get(i);
			if (ain.getType() == AbstractInsnNode.LABEL) {
				LabelNode label = (LabelNode) ain;
				JRadioButton btn = new JRadioButton(i + ".");
				labels.put(btn, label);
				if (label.equals(initial)) {
					btn.setSelected(true);
				}
				btn.addActionListener(new RadioListener(btn));
				content.add(btn);
			}
		}
	}

	/**
	 * Listener for disabling other radio buttons.
	 * 
	 * @author Matt
	 */
	private class RadioListener implements ActionListener {
		private final JRadioButton btn;

		public RadioListener(JRadioButton btn) {
			this.btn = btn;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			for (JRadioButton comp : labels.keySet()) {
				if (comp != btn && comp.isSelected()) {
					comp.setSelected(false);
				} else if (comp == btn) {
					updater.accept(labels.get(comp));
					list.repaint();
				}
			}
		}
	}
}
