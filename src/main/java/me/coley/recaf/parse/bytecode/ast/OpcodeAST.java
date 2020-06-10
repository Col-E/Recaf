package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.util.OpcodeUtil;

/**
 * Instruction name AST.
 *
 * @author Matt
 */
public class OpcodeAST extends AST {
	private final String name;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param name
	 * 		Opcode display name.
	 */
	public OpcodeAST(int line, int start, String name) {
		super(line, start);
		this.name = name;
	}

	/**
	 * @return Opcode display name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Opcode value.
	 */
	public int getOpcode() {
		return OpcodeUtil.nameToOpcode(getName());
	}

	@Override
	public String print() {
		return name;
	}
}
