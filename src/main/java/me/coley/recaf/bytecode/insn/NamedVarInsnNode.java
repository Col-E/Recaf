package me.coley.recaf.bytecode.insn;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Extension of VarInsnNode that uses a string as the variable index, which is resolved later.
 *
 * @author Matt
 */
public class NamedVarInsnNode extends VarInsnNode implements NamedVarRefInsn {
	/**
	 * Placeholder variable identifier.
	 */
	private final String varId;

	public NamedVarInsnNode(int opcode, String varId) {
		super(opcode, -1);
		this.varId = varId;
	}

	@Override
	public AbstractInsnNode clone(Var v) {
		return new VarInsnNode(opcode, v.index);
	}

	@Override
	public String getVarName() {
		return varId;
	}

}
