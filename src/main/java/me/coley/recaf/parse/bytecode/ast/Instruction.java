package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.AssemblerException;
import me.coley.recaf.parse.bytecode.Variables;
import me.coley.recaf.util.OpcodeUtil;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.Map;

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
	 * @param labels
	 * 		Label name to instances.
	 * @param variables
	 * 		Variable information.
	 *
	 * @return ASM instruction.
	 *
	 * @throws AssemblerException
	 * 		When conversion to insn failed.
	 */
	AbstractInsnNode compile(Map<String, LabelNode> labels, Variables variables) throws AssemblerException;
}
