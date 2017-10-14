package me.coley.recaf.ui.component.panel;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

import me.coley.recaf.asm.OpcodeUtil;
import me.coley.recaf.util.Misc;

/**
 * JPanel for tag switcher for Handle.
 *
 * @author Matt
 */
@SuppressWarnings("serial")
public class TagTypeSwitchPanel extends JPanel implements Opcodes {
	/**
	 * Map of radio buttons to possible tags.
	 */
	private final Map<JRadioButton, Integer> compToTag = new HashMap<>();
	/**
	 * InvokeDynamic handle being modified.
	 */
	private final Handle handle;
	/**
	 * Only stored so the list can be re-painted when the tag is re-chosen.
	 */
	private final JList<AbstractInsnNode> list;
	/**
	 * Content wrapper.
	 */
	private final JPanel content = new JPanel();

	public TagTypeSwitchPanel(JList<AbstractInsnNode> list, Handle handle) {
		this.list = list;
		this.handle = handle;
		//setMaximumSize(new Dimension(300, 300));
		// content.setMaximumSize(new Dimension(300, 300));
		// scroll.setMaximumSize(new Dimension(300, 300));
		content.setLayout(new GridLayout(0, 2));
		populate(OpcodeUtil.OPS_TAG, s -> OpcodeUtil.nameToTag(s));
		setLayout(new BorderLayout());
		JScrollPane scroll = new JScrollPane(content);
		add(scroll, BorderLayout.CENTER);
	}

	private void populate(String[] tags, Function<String, Integer> getter) {
		// Set layout based on number of options
		// Add options
		for (String op : tags) {
			int value = getter.apply(op);;
			JRadioButton btn = new JRadioButton(op);
			if (value == handle.getTag()) {
				btn.setSelected(true);
			}
			btn.addActionListener(new RadioListener(btn));
			compToTag.put(btn, value);
			content.add(btn);
		}
	}

	/**
	 * Update {@link #handle} value.
	 *
	 * @param value
	 */
	private void setValue(int value) {
		Misc.set(handle, "tag", value);
		list.repaint();
	}

	/**
	 * @return The number of radio buttons.

	 */
	public int getOptionCount() {
		return compToTag.keySet().size();
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
			for (JRadioButton comp : compToTag.keySet()) {
				if (comp != btn && comp.isSelected()) {
					comp.setSelected(false);
				} else if (comp == btn) {
					setValue(compToTag.get(comp));
				}
			}
		}
	}
}
