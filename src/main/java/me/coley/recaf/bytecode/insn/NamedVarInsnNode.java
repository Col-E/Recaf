package me.coley.recaf.bytecode.insn;

import org.objectweb.asm.tree.*;

import java.util.Map;

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
	public AbstractInsnNode clone(final Map<LabelNode, LabelNode> clonedLabels) {
		return new NamedVarInsnNode(opcode, varId).cloneAnnotations(this);
	}

	@Override
	public String getVarName() {
		return varId;
	}

}
