package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.MethodCompilation;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;

/**
 * Alias AST.
 *
 * @author Matt
 */
public class AliasAST extends InsnAST {
	private final NameAST name;
	private final StringAST value;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param opcode
	 * 		Opcode AST.
	 * @param name
	 * 		Alias name AST.
	 * @param value
	 * 		Increment value AST.
	 */
	public AliasAST(int line, int start, OpcodeAST opcode, NameAST name, StringAST value) {
		super(line, start, opcode);
		this.name = name;
		this.value = value;
		addChild(name);
		addChild(value);
	}

	/**
	 * @return Alias name AST.
	 */
	public NameAST getName() {
		return name;
	}

	/**
	 * @return Increment value AST.
	 */
	public StringAST getValue() {
		return value;
	}

	@Override
	public String print() {
		return getOpcode().print() + " " + name.print() + " " + value.print();
	}

	@Override
	public void compile(MethodCompilation compilation) throws AssemblerException {
		// No-op: this is not compilable.
	}
}
