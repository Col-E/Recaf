package me.coley.recaf.code;

import com.github.javaparser.ast.expr.Expression;

/**
 * Wrapper around a {@link Expression} denoting a literal value.
 *
 * @author Amejonah
 */
public class LiteralExpressionInfo implements ItemInfo {
	private final Number value;
	private final Expression expression;

	/**
	 * @param value
	 * 		Literal value.
	 * @param expression
	 * 		Expression of the value.
	 */
	public LiteralExpressionInfo(Number value, Expression expression) {
		this.value = value;
		this.expression = expression;
	}

	@Override
	public String getName() {
		return "expression";
	}

	/**
	 * @return Literal value.
	 */
	public Number getValue() {
		return value;
	}

	/**
	 * @return Expression of the value.
	 */
	public Expression getExpression() {
		return expression;
	}
}
