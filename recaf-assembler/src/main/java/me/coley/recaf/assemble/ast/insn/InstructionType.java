package me.coley.recaf.assemble.ast.insn;

import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * The ASM group <i>({@link AbstractInsnNode#getType()})</i> of an AST type.
 *
 * @author Matt Coley
 */
public enum InstructionType {
	FIELD,
	METHOD,
	INDY,
	VAR,
	IINC,
	INT,
	LDC,
	TYPE,
	MULTIARRAY,
	NEWARRAY,
	INSN,
	JUMP,
	LOOKUP,
	TABLE,
	LABEL,
	LINE,
	EXPRESSION
}
