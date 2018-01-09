package me.coley.recaf.asm.tracker;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

/**
 * MethodNode implementation that keeps track of when opcodes are modified in the
 * list. Allows for the agent-mode to only redefine classes that have been
 * modified.
 * 
 * @author Matt
 */
public class TrackingMethodNode extends MethodNode {
	private final TrackingClassNode clazz;

	public TrackingMethodNode(TrackingClassNode clazz, int access, String name, String desc, String signature,
			String[] exceptions) {
		super(Opcodes.ASM6, access, name, desc, signature, exceptions);
		this.clazz = clazz;
		this.instructions = new TrackingInsnList(this);
	}
	
	@Override
	public void visitEnd() {
		((TrackingInsnList)instructions).setUpdateable();
	}

	public TrackingClassNode getClazz() {
		return clazz;
	}
}
