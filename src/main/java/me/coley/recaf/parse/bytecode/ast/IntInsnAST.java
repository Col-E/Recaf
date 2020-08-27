package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.MethodCompilation;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import me.coley.recaf.util.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * Int instruction AST.
 *
 * @author Matt
 */
public class IntInsnAST extends InsnAST {
	private final NumberAST value;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param opcode
	 * 		Opcode AST.
	 * @param value
	 * 		Int value.
	 */
	public IntInsnAST(int line, int start, OpcodeAST opcode, NumberAST value) {
		super(line, start, opcode);
		this.value = value;
		addChild(value);
	}

	/**
	 * @return value AST.
	 */
	public NumberAST getValue() {
		return value;
	}

	@Override
	public String print() {
		if (getOpcode().getOpcode() == Opcodes.NEWARRAY) {
			return getOpcode().print() + " " + TypeUtil.newArrayArgToType(value.getIntValue()).getDescriptor();
		} else {
			return getOpcode().print() + " " + value.print();
		}
	}

	@Override
	public void compile(MethodCompilation compilation) throws AssemblerException {
		compilation.addInstruction(new IntInsnNode(getOpcode().getOpcode(), getValue().getIntValue()), this);
	}
}
