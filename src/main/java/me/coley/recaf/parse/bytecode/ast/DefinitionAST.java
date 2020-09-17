package me.coley.recaf.parse.bytecode.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Common base for definition AST objects.
 *
 * @author Matt
 */
public abstract class DefinitionAST extends AST {
	protected final List<DefinitionModifierAST> modifiers = new ArrayList<>();
	protected final NameAST name;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param name
	 * 		Member name.
	 */
	public DefinitionAST(int line, int start, NameAST name) {
		super(line, start);
		this.name = name;
	}

	/**
	 * @return Full descriptor of the definition.
	 */
	public abstract String getDescriptor();

	/**
	 * @return Method name.
	 */
	public NameAST getName() {
		return name;
	}
}
