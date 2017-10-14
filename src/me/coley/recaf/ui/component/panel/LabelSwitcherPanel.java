package me.coley.recaf.ui.component.panel;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.ui.component.list.OpcodeList;

/**
 * JPanel for updating label values.
 *
 * @author Matt
 */
@SuppressWarnings("serial")
public class LabelSwitcherPanel extends JPanel implements Opcodes {
	/**
	 * Map of strings to labels.
	 */
	private final Map<String, LabelNode> labels = new LinkedHashMap<>();
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
	private final OpcodeList list;
	/**
	 * Combobox containing label node indices.
	 */
	private final JComboBox<String> combo = new JComboBox<>();

	public LabelSwitcherPanel(MethodNode method, LabelNode initial, Consumer<LabelNode> updater) {
		this(null, method, initial, updater);
	}

	public LabelSwitcherPanel(OpcodeList list, MethodNode method, LabelNode initial, Consumer<LabelNode> updater) {
		this.initial = initial;
		this.updater = updater;
		this.list = list;
		setLayout(new BorderLayout());
		populate(method.instructions);
		add(combo, BorderLayout.CENTER);
	}

	private void populate(InsnList opcodes) {
		int selected = -1, labelCount = 0;;
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
		for (int i = 0; i < opcodes.size(); i++) {
			AbstractInsnNode ain = opcodes.get(i);
			if (ain.getType() == AbstractInsnNode.LABEL) {
				LabelNode label = (LabelNode) ain;
				String s = i + ".";
				if (list != null) {
					s += " : " + list.getLabelName(ain);
				}
				labels.put(s, label);
				model.addElement(s);
				if (label.equals(initial)) {
					selected = labelCount;
				}
				labelCount++;
			}
		}
		combo.setModel(model);
		combo.setSelectedIndex(selected);
		combo.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				updater.accept(labels.get(e.getItem()));
				if (list != null) {
					list.repaint();
				}
			}
		});
	}
}
