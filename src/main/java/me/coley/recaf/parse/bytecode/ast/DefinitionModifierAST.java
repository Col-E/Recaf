package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.util.AccessFlag;

/**
 * Method modifier AST.
 *
 * @author Matt
 */
public class DefinitionModifierAST extends AST {
	private final String name;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param name
	 * 		Modifier name.
	 */
	public DefinitionModifierAST(int line, int start, String name) {
		super(line, start);
		this.name = name;
	}

	/**
	 * @return Modifier name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Value of modifier
	 */
	public int getValue() {
		return AccessFlag.getFlag(getName()).getMask();
	}

	@Override
	public String print() {
		return name;
	}
}
