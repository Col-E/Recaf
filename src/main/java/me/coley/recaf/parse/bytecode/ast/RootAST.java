package me.coley.recaf.parse.bytecode.ast;

import java.util.stream.Collectors;

/**
 * Root AST.
 *
 * @author Matt
 */
public class RootAST extends AST {
	/**
	 * Create root.
	 */
	public RootAST() {
		super(0,0);
	}

	@Override
	public String print() {
		return getChildren().stream().map(AST::print).collect(Collectors.joining("\n"));
	}
}
