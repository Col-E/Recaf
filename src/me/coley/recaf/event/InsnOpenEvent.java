package me.coley.recaf.event;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.event.Event;

/**
 * Event for when a method opcode is selected.
 * 
 * @author Matt
 */
public class InsnOpenEvent extends Event {
	private final ClassNode owner;
	private final MethodNode method;
	private final AbstractInsnNode insn;

	public InsnOpenEvent(ClassNode owner, MethodNode method, AbstractInsnNode insn) {
		this.owner = owner;
		this.method = method;
		this.insn = insn;
	}

	/**
	 * @return ClassNode that contains the {@link #getMethod() method}.
	 */
	public ClassNode getOwner() {
		return owner;
	}

	/**
	 * @return Method selected that contains the {@link #getInsn() instruction}.
	 */
	public MethodNode getMethod() {
		return method;
	}

	/**
	 * @return Instruction selected.
	 */
	public AbstractInsnNode getInsn() {
		return insn;
	}
}