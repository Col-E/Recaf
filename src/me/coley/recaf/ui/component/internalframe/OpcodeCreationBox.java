package me.coley.recaf.ui.component.internalframe;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import me.coley.recaf.ui.component.action.ActionButton;

@SuppressWarnings("serial")
public class OpcodeCreationBox extends BasicFrame {

	public OpcodeCreationBox(boolean insertBefore, InsnList list,  AbstractInsnNode target) {
		super("Create Opcode");
		setLayout(new BorderLayout());
		JPanel content = new JPanel();
		// TODO: How to auto-generate most of the stuff needed per-opcode
		ActionButton btn = new ActionButton("Add Opcode", () -> {
			AbstractInsnNode ain = getCreatedInsn();
			if (ain != null) {
				if (insertBefore) {
					list.insertBefore(target, ain);
				} else {
					list.insert(target, ain);
				}
			}
		});
		add(btn, BorderLayout.CENTER);
	}

	public AbstractInsnNode getCreatedInsn() {
		return null;
	}
}
