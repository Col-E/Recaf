package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.util.OpcodeUtil;
import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * Common to all instruction AST.
 *
 * @author Matt
 */
public interface Instruction extends Compilable {
	/**
	 * @return Opcode AST.
	 */
	OpcodeAST getOpcode();

	/**
	 * @return Instruction group. See {@link AbstractInsnNode#getType()}
	 */
	default int getInsnType() {
		return OpcodeUtil.opcodeToType(getOpcode().getOpcode());
	}
}
