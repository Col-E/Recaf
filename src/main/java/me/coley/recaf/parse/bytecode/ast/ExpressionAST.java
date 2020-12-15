package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.MethodCompilation;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;

/**
 * A one-liner source level expression.
 *
 * @author Matt
 */
public class ExpressionAST extends AST implements Compilable {
	private final String expression;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param expression
	 * 		Expression literal value.
	 */
	public ExpressionAST(int line, int start, String expression) {
		super(line, start);
		this.expression = expression;
	}

	/**
	 * @return Expression literal value.
	 */
	public String getExpression() {
		return expression;
	}

	@Override
	public String print() {
		return "EXPR " + expression;
	}


	@Override
	public void compile(MethodCompilation compilation) throws AssemblerException {
		compilation.addExpression(expression, this);
	}
}
