package me.coley.recaf.ui.component.list;

import java.awt.Color;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import me.coley.recaf.Program;

public class OpcodeSelectionListener implements ListSelectionListener, Opcodes {
	// TODO: Are these needed?
	@SuppressWarnings("unused")
	private final Program callback;
	@SuppressWarnings("unused")
	private final MethodNode method;
	private static final Color colJumpFail = new Color(250, 200, 200);
	private static final Color colJumpSuccess = new Color(200, 250, 200);

	public OpcodeSelectionListener(MethodNode method, Program callback) {
		this.method = method;
		this.callback = callback;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (!e.getValueIsAdjusting()) {
			return;
		}
		OpcodeList list = (OpcodeList) e.getSource();
		boolean multiple = list.getMaxSelectionIndex() != list.getMinSelectionIndex();
		AbstractInsnNode selected = list.getSelectedValue();
		list.getColorMap().clear();
		list.repaint();
		if (!multiple && selected != null) {
			int op = selected.getOpcode();
			if (selected instanceof JumpInsnNode) {
				JumpInsnNode insnJump = (JumpInsnNode) selected;
				if (op != GOTO && op != JSR) {
					list.getColorMap().put(insnJump.getNext(), colJumpFail);
				}
				list.getColorMap().put(insnJump.label, colJumpSuccess);
			}
		}
	}

}
