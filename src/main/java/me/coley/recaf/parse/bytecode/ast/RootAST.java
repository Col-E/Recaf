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

	/**
	 * @param line
	 * 		Line number.
	 *
	 * @return AST at line number. May be {@code null}.
	 */
	public AST getAtLine(int line) {
		return getChildren().stream()
					.filter(ast -> ast.getLine() == line)
					.findFirst().orElse(null);
	}

	@Override
	public String print() {
		return getChildren().stream().map(AST::print).collect(Collectors.joining("\n"));
	}
}
