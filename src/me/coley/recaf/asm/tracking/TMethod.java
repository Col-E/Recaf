package me.coley.recaf.asm.tracking;

import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.Recaf;

/**
 * MethodNode implementation that keeps track of when opcodes are modified in the
 * list. Allows for the agent-mode to only redefine classes that have been
 * modified.
 * 
 * @author Matt
 */
public class TMethod extends MethodNode {
	private final TClass clazz;

	public TMethod(TClass clazz, int access, String name, String desc, String signature,
			String[] exceptions) {
		super(Recaf.INSTANCE.configs.asm.version, access, name, desc, signature, exceptions);
		this.clazz = clazz;
		this.instructions = new TInsnList(this);
	}
	
	@Override
	public void visitEnd() {
		((TInsnList)instructions).setUpdateable();
	}

	public TClass getClazz() {
		return clazz;
	}
}
