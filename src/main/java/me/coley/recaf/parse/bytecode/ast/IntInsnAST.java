package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.Variables;
import org.objectweb.asm.tree.*;

import java.util.Map;

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
		return getOpcode().print() + " " + value.print();
	}

	@Override
	public AbstractInsnNode compile(Map<String, LabelNode> labels, Variables variables) {
		return new IntInsnNode(getOpcode().getOpcode(), getValue().getIntValue());
	}
}
