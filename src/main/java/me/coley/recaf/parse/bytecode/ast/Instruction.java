package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.MethodCompilation;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import me.coley.recaf.util.OpcodeUtil;
import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * Common to all instruction AST.
 *
 * @author Matt
 */
public interface Instruction {
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

	/**
	 * @param compilation
	 * 		Compilation context.
	 *
	 * @return ASM instruction.
	 *
	 * @throws AssemblerException
	 * 		When conversion to insn failed.
	 */
	AbstractInsnNode compile(MethodCompilation compilation) throws AssemblerException;
}
