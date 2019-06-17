package me.coley.recaf.bytecode.insn;

import org.objectweb.asm.tree.*;

import java.util.Map;

/**
 * Extension of IincInsnNode that uses a string as the variable index, which is resolved later.
 *
 * @author Matt
 */
public class NamedIincInsnNode extends IincInsnNode implements NamedVarRefInsn {
	/**
	 * Placeholder variable identifier.
	 */
	private final String varId;

	public NamedIincInsnNode(int incr, String varId) {
		super(-1, incr);
		this.varId = varId;
	}

	@Override
	public AbstractInsnNode clone(Var v) {
		return new IincInsnNode(v.index, incr);
	}

	@Override
	public AbstractInsnNode clone(final Map<LabelNode, LabelNode> clonedLabels) {
		return new NamedIincInsnNode(incr, varId).cloneAnnotations(this);
	}

	@Override
	public String getVarName() {
		return varId;
	}

}
