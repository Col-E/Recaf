package me.coley.recaf.event.impl;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.event.Event;
import me.coley.recaf.ui.component.list.OpcodeList;
import me.coley.recaf.ui.component.panel.ClassDisplayPanel;

/**
 * Created when: User opens a method's opcodes.
 * 
 * @author Matt
 */
public class EMethodSelect extends Event {
	private final ClassDisplayPanel cdp;
	private final OpcodeList opcodes;

	public EMethodSelect(ClassDisplayPanel cdp, OpcodeList opcodes) {
		this.cdp = cdp;
		this.opcodes = opcodes;
	}

	/**
	 * @return Panel containing class editing UI for the {@link #getClassNode()
	 *         class}.
	 */
	public ClassDisplayPanel getDisplay() {
		return cdp;
	}

	/**
	 * @return List display for the {@link #getMethodNode() method}.
	 */
	public OpcodeList getOpcodeDisplay() {
		return opcodes;
	}

	/**
	 * @return ClassNode containing {@link #getMethodNode()}.
	 */
	public ClassNode getClassNode() {
		return cdp.getNode();
	}

	/**
	 * @return MethodNode of {@link #getOpcodeDisplay() opcode list}.
	 */
	public MethodNode getMethodNode() {
		return opcodes.getMethod();
	}
}
