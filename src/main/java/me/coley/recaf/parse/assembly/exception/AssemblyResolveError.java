package me.coley.recaf.parse.assembly.exception;

import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * Exception for custom instructions that failed to have variable names resolved to an index.
 *
 * @author Matt
 */
public class AssemblyResolveError extends AssemblyParseException {
	private final AbstractInsnNode insn;

	public AssemblyResolveError(AbstractInsnNode insn, String message) {
		super(message);
		this.insn = insn;
	}

	/**
	 * Returns the instruction that failed linkage.
	 * The stored label could not be linked to a cloned label.
	 *
	 * @return Offending instruction.
	 */
	public AbstractInsnNode getInsn() {
		return insn;
	}
}
