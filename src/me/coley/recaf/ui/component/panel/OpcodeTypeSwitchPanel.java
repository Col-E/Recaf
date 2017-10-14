package me.coley.recaf.ui.component.panel;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

import me.coley.recaf.asm.OpcodeUtil;

/**
 * JPanel for opcode switcher for AbstractInsnNode.
 *
 * @author Matt
 */
@SuppressWarnings("serial")
public class OpcodeTypeSwitchPanel extends JPanel implements Opcodes {
	/**
	 * Map of radio buttons to possible opcodes.
	 */
	private final Map<JRadioButton, Integer> compToOpcode = new HashMap<>();
	/**
	 * Opcode node being modified.
	 */
	private final AbstractInsnNode opcode;
	/**
	 * Reference so list can be re-painted.
	 */
	private final JList<AbstractInsnNode> list;
	/**
	 * Content wrapper.
	 */
	private final JPanel content = new JPanel();

	public OpcodeTypeSwitchPanel(JList<AbstractInsnNode> list, AbstractInsnNode opcode) {
		this.list = list;
		this.opcode = opcode;
		// setMaximumSize(new Dimension(900, 200));
		// content.setMaximumSize(new Dimension(900, 300));
		// scroll.setMaximumSize(new Dimension(900, 300));
		populate();
		setLayout(new BorderLayout());
		JScrollPane scroll = new JScrollPane(content);
		add(scroll, BorderLayout.CENTER);
	}

	private void populate() {
		switch (opcode.getType()) {
		case AbstractInsnNode.INSN:
			populate(OpcodeUtil.getInsnSubset(OpcodeUtil.opcodeToName(opcode.getOpcode())));
			break;
		case AbstractInsnNode.INT_INSN:
			populate(OpcodeUtil.OPS_INT);
			break;
		case AbstractInsnNode.VAR_INSN:
			populate(OpcodeUtil.OPS_VAR);
			break;
		case AbstractInsnNode.TYPE_INSN:
			populate(OpcodeUtil.OPS_TYPE);
			break;
		case AbstractInsnNode.FIELD_INSN:
			populate(OpcodeUtil.OPS_FIELD);
			break;
		case AbstractInsnNode.METHOD_INSN:
			populate(OpcodeUtil.OPS_METHOD);
			break;
		case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
			populate(OpcodeUtil.OPS_INDY_METHOD);
			break;
		case AbstractInsnNode.JUMP_INSN:
			populate(OpcodeUtil.OPS_JUMP);
			break;
		case AbstractInsnNode.LDC_INSN:
			populate(OpcodeUtil.OPS_LDC);
			break;
		case AbstractInsnNode.IINC_INSN:
			populate(OpcodeUtil.OPS_IINC);
			break;
		case AbstractInsnNode.TABLESWITCH_INSN:
			populate(OpcodeUtil.OPS_TABLESWITCH);
			break;
		case AbstractInsnNode.LOOKUPSWITCH_INSN:
			populate(OpcodeUtil.OPS_LOOKUPSWITCH);
			break;
		case AbstractInsnNode.MULTIANEWARRAY_INSN:
			populate(OpcodeUtil.OPS_MULTIANEWARRAY);
			break;
		case AbstractInsnNode.LABEL:
		case AbstractInsnNode.LINE:
			break;
		case AbstractInsnNode.FRAME:
			populateFrames(OpcodeUtil.OPS_FRAME);
			break;
		}
	}

	private void populate(String[] opcodes) {
		populate(opcodes, s -> OpcodeUtil.nameToOpcode(s));

	}

	private void populateFrames(String[] opcodes) {
		populate(opcodes, s -> OpcodeUtil.nameToFrame(s));
	}

	private void populate(String[] opcodes, Function<String, Integer> getter) {
		// don't bother showing the option to change if there are no other
		// options
		if (opcodes.length == 1) {
			return;
		}
		// Set layout based on number of options
		setLayout(opcodes.length);
		// Add options
		for (String op : opcodes) {
			int value = getter.apply(op);;
			JRadioButton btn = new JRadioButton(op);
			if (value == opcode.getOpcode()) {
				btn.setSelected(true);
			}
			btn.addActionListener(new RadioListener(btn));
			compToOpcode.put(btn, value);
			content.add(btn);
		}
	}

	/**
	 * Update {@link #opcode} value.
	 *
	 * @param value
	 */
	private void setValue(int value) {
		try {
			Field op = AbstractInsnNode.class.getDeclaredField("opcode");
			op.setAccessible(true);
			op.set(opcode, value);
			list.repaint();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sets layout depending on the content size.
	 *
	 * @param size
	 */
	private void setLayout(int size) {
		if (size % 6 == 0 || size % 3 == 0) {
			content.setLayout(new GridLayout(0, 3));
		} else if (size % 2 == 0) {
			content.setLayout(new GridLayout(0, 2));
		} else {
			content.setLayout(new GridLayout(0, 3));
		}
	}

	/**
	 * @return the number of radio buttons.

	 */
	public int getOptionCount() {
		return compToOpcode.keySet().size();
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
			for (JRadioButton comp : compToOpcode.keySet()) {
				if (comp != btn && comp.isSelected()) {
					comp.setSelected(false);
				} else if (comp == btn) {
					setValue(compToOpcode.get(comp));
				}
			}
		}
	}
}
