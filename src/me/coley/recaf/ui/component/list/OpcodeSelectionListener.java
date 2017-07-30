package me.coley.recaf.ui.component.list;

import java.awt.Color;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class OpcodeSelectionListener implements ListSelectionListener, Opcodes {
	private static final Color colJumpFail = new Color(250, 200, 200);
	private static final Color colJumpSuccess = new Color(200, 250, 200);
	private static final Color colJumpRange = new Color(220, 220, 170);

	@Override
	public void valueChanged(ListSelectionEvent e) {
		// TODO: getValueIsAdjusting = true for keyboard up/down
		// getValueIsAdjusting = false for mouse press (true release)
		//
		// Should find a way so this isn't called twice but is instant for both.
		/*
		 * if (e.getValueIsAdjusting()) { return; }
		 */
		OpcodeList list = (OpcodeList) e.getSource();
		boolean multiple = list.getMaxSelectionIndex() != list.getMinSelectionIndex();
		AbstractInsnNode selected = list.getSelectedValue();
		list.getColorMap().clear();
		list.getAppendMap().clear();
		list.repaint();
		if (!multiple && selected != null) {
			int op = selected.getOpcode();
			switch (selected.getType()) {
			case AbstractInsnNode.JUMP_INSN:
				JumpInsnNode insnJump = (JumpInsnNode) selected;
				if (op != GOTO && op != JSR) {
					list.getColorMap().put(insnJump.getNext(), colJumpFail);
				}
				list.getColorMap().put(insnJump.label, colJumpSuccess);
				break;
			case AbstractInsnNode.TABLESWITCH_INSN:
				TableSwitchInsnNode insnTableSwitch = (TableSwitchInsnNode) selected;
				int diff = insnTableSwitch.max - insnTableSwitch.min;
				for (int i = 0; i <= diff; i++) {
					int key = i + insnTableSwitch.min;
					LabelNode label =  insnTableSwitch.labels.get(i);
					list.getAppendMap().put(label, " [switch key: " + key + "]");
					list.getColorMap().put(label, colJumpRange);
				}
				for (LabelNode label : insnTableSwitch.labels) {
					list.getColorMap().put(label, colJumpRange);
				}
				list.getColorMap().put(insnTableSwitch.dflt, colJumpFail);
				break;
			case AbstractInsnNode.LOOKUPSWITCH_INSN:
				LookupSwitchInsnNode insnLookupSwitch = (LookupSwitchInsnNode) selected;
				for (int i = 0; i < insnLookupSwitch.keys.size(); i++) {
					int key = insnLookupSwitch.keys.get(i);
					LabelNode label = insnLookupSwitch.labels.get(i);
					list.getAppendMap().put(label, " [switch key: " + key + "]");
					list.getColorMap().put(label, colJumpRange);
				}
				list.getColorMap().put(insnLookupSwitch.dflt, colJumpFail);
				break;
			}
		}
	}

}
