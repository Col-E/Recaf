package me.coley.recaf.ui.component.list;

import java.awt.Color;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import me.coley.recaf.Recaf;
import me.coley.recaf.config.Colors;

public class OpcodeSelectionListener implements ListSelectionListener, Opcodes {
	@Override
	public void valueChanged(ListSelectionEvent e) {
		// TODO: getValueIsAdjusting = true for keyboard up/down
		// getValueIsAdjusting = false for mouse press (true release)
		//
		// Should find a way so this isn't called twice but is instant for both.
		/*
		 * if (e.getValueIsAdjusting()) { return; }
		 */
		Colors colors = Recaf.INSTANCE.colors;
		OpcodeList list = (OpcodeList) e.getSource();
		boolean multiple = list.getMaxSelectionIndex() != list.getMinSelectionIndex();
		AbstractInsnNode selected = list.getSelectedValue();
		list.getColorMap().clear();
		list.getAppendMap().clear();
		list.repaint();
		if (!multiple && selected != null) {
			int op = selected.getOpcode();
			switch (selected.getType()) {
			case AbstractInsnNode.LABEL:
				MethodNode method = list.getMethod();
				for (AbstractInsnNode ain : method.instructions.toArray()) {
					if (ain.getType() == AbstractInsnNode.JUMP_INSN) {
						JumpInsnNode insnJump = (JumpInsnNode) ain;
						if (insnJump.label.equals(selected)) {
							list.getColorMap().put(insnJump, Color.decode(colors.opcodeLabelJumpReferrer));
						}
					} else if (ain.getType() == AbstractInsnNode.TABLESWITCH_INSN) {
						TableSwitchInsnNode insnTableSwitch = (TableSwitchInsnNode) ain;
						if (selected.equals(insnTableSwitch.dflt)) {
							list.getColorMap().put(insnTableSwitch, Color.decode(colors.opcodeHighlightJumpRange));
						}
						for (LabelNode ln : insnTableSwitch.labels) {
							if (selected.equals(ln)) {
								list.getColorMap().put(insnTableSwitch, Color.decode(colors.opcodeHighlightJumpRange));
							}
						}
					} else if (ain.getType() == AbstractInsnNode.LOOKUPSWITCH_INSN) {
						LookupSwitchInsnNode insnLookupSwitch = (LookupSwitchInsnNode) ain;
						if (selected.equals(insnLookupSwitch.dflt)) {
							list.getColorMap().put(insnLookupSwitch, Color.decode(colors.opcodeHighlightJumpRange));
						}
						for (LabelNode ln : insnLookupSwitch.labels) {
							if (selected.equals(ln)) {
								list.getColorMap().put(insnLookupSwitch, Color.decode(colors.opcodeHighlightJumpRange));
							}
						}
					}
				}
				break;
			case AbstractInsnNode.JUMP_INSN:
				JumpInsnNode insnJump = (JumpInsnNode) selected;
				if (op != GOTO && op != JSR) {
					list.getColorMap().put(insnJump.getNext(), Color.decode(colors.opcodeHighlightJumpFail));
				}
				list.getColorMap().put(insnJump.label, Color.decode(colors.opcodeHighlightJumpSuccess));
				break;
			case AbstractInsnNode.TABLESWITCH_INSN:
				TableSwitchInsnNode insnTableSwitch = (TableSwitchInsnNode) selected;
				int diff = insnTableSwitch.max - insnTableSwitch.min;
				for (int i = 0; i <= diff; i++) {
					int key = i + insnTableSwitch.min;
					LabelNode label =  insnTableSwitch.labels.get(i);
					list.getAppendMap().put(label, " [switch key: " + key + "]");
					list.getColorMap().put(label, Color.decode(colors.opcodeHighlightJumpRange));
				}
				for (LabelNode label : insnTableSwitch.labels) {
					list.getColorMap().put(label, Color.decode(colors.opcodeHighlightJumpRange));
				}
				list.getColorMap().put(insnTableSwitch.dflt, Color.decode(colors.opcodeHighlightJumpFail));
				break;
			case AbstractInsnNode.LOOKUPSWITCH_INSN:
				LookupSwitchInsnNode insnLookupSwitch = (LookupSwitchInsnNode) selected;
				for (int i = 0; i < insnLookupSwitch.keys.size(); i++) {
					int key = insnLookupSwitch.keys.get(i);
					LabelNode label = insnLookupSwitch.labels.get(i);
					list.getAppendMap().put(label, " [switch key: " + key + "]");
					list.getColorMap().put(label, Color.decode(colors.opcodeHighlightJumpRange));
				}
				list.getColorMap().put(insnLookupSwitch.dflt, Color.decode(colors.opcodeHighlightJumpFail));
				break;
			}
		}
	}

}
