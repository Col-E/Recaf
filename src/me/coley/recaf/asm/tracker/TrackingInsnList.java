package me.coley.recaf.asm.tracker;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import me.coley.recaf.agent.Marker;

/**
 * InsnList implementation that keeps track of when opcodes are modified in the
 * list. Allows for the agent-mode to only redefine classes that have been
 * modified.
 * 
 * @author Matt
 */
// TODO: Should get operations cause marking?
public class TrackingInsnList extends InsnList {
	private final TrackingMethodNode method;
	/**
	 * Flag for allowing marking.
	 */
	private boolean canUpdate;

	public TrackingInsnList(TrackingMethodNode method) {
		this.method = method;
	}

	@Override
	public void set(final AbstractInsnNode location, final AbstractInsnNode insn) {
		super.set(location, insn);
		setModified();
	}

	@Override
	public void add(final AbstractInsnNode insn) {
		super.add(insn);
		setModified();
	}

	@Override
	public void add(final InsnList insns) {
		super.add(insns);
		setModified();
	}

	@Override
	public void insert(final AbstractInsnNode insn) {
		super.insert(insn);
		setModified();
	}

	@Override
	public void insert(final InsnList insns) {
		super.insert(insns);
		setModified();
	}

	@Override
	public void insert(final AbstractInsnNode location, final AbstractInsnNode insn) {
		super.insert(location, insn);
		setModified();
	}

	@Override
	public void insert(final AbstractInsnNode location, final InsnList insns) {
		super.insert(location, insns);
		setModified();
	}

	@Override
	public void insertBefore(final AbstractInsnNode location, final AbstractInsnNode insn) {
		super.insertBefore(location, insn);
		setModified();
	}

	@Override
	public void insertBefore(final AbstractInsnNode location, final InsnList insns) {
		super.insertBefore(location, insns);
		setModified();
	}

	@Override
	public void remove(final AbstractInsnNode insn) {
		super.remove(insn);
		setModified();
	}

	@Override
	public void clear() {
		super.clear();
		setModified();
	}

	/**
	 * Marks the class that owns this code as modified.
	 */
	public final void setModified() {
		if (canUpdate) {
			Marker.mark(method.getClazz().name);
		}
	}

	/**
	 * Allows marking.
	 */
	public void setUpdateable() {
		canUpdate = true;
	}
}