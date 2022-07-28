package me.coley.recaf.parse.evaluation;

import com.github.javaparser.ast.expr.*;

import java.util.Optional;

/**
 * Utilities to evaluate {@link Expression} values.
 *
 * @author Amejonah
 */
public class ExpressionEvaluator {
	private ExpressionEvaluator() {
	}

	/**
	 * @param binOp
	 * 		Expression to evaluate. Represents a mathematical operation with two inputs.
	 *
	 * @return Wrapped value of the expression, or {@link Optional#empty()} if the expression could not be evaluated.
	 */
	public static Optional<Number> evaluate(BinaryExpr binOp) {
		Optional<Number> leftValue = evaluate(binOp.getLeft());
		Optional<Number> rightValue = evaluate(binOp.getRight());
		if (leftValue.isPresent() && rightValue.isPresent()) {
			return NumberEvaluator.evaluate(leftValue.get(), binOp.getOperator(), rightValue.get());
		}
		return Optional.empty();
	}

	/**
	 * @param unaryExpr
	 * 		Expression to evaluate. Represents a mathematical operation with one input.
	 *
	 * @return Wrapped value of the expression, or {@link Optional#empty()} if the expression could not be evaluated.
	 */
	public static Optional<Number> evaluate(UnaryExpr unaryExpr) {
		return evaluate(unaryExpr.getExpression())
				.flatMap(n -> NumberEvaluator.evaluate(unaryExpr.getOperator(), n));
	}

	/**
	 * @param literalExpr
	 * 		Expression to evaluate.
	 *
	 * @return Wrapped value of the expression, or {@link Optional#empty()} if the expression could not be evaluated.
	 */
	public static Optional<Number> evaluate(LiteralExpr literalExpr) {
		try {
			// No byte, short: JavaParser only supports exporting to 'int'
			// No float: JavaParser only supports exporting to 'double'
			return literalExpr.toIntegerLiteralExpr().map(IntegerLiteralExpr::asNumber)
					.or(() -> literalExpr.toDoubleLiteralExpr().map(DoubleLiteralExpr::asDouble))
					.or(() -> literalExpr.toLongLiteralExpr().map(LongLiteralExpr::asNumber));
		} catch (NumberFormatException e) {
			// If the value range an int or long literal is exceeded, it will throw a NumberFormatException,
			// therefore this is caught and stop evaluation.
			return Optional.empty();
		}
	}

	/**
	 * @param castExpr
	 * 		Expression to evaluate.
	 *
	 * @return Wrapped value of the expression, or {@link Optional#empty()} if the expression could not be evaluated.
	 */
	public static Optional<Number> evaluate(CastExpr castExpr) {
		return evaluate(castExpr.getExpression())
				.flatMap(n -> castExpr.getType().toPrimitiveType()
						.flatMap(t -> NumberEvaluator.cast(n, t)));
	}

	/**
	 * @param expr
	 * 		Expression to evaluate.
	 *
	 * @return Wrapped value of the expression, or {@link Optional#empty()} if the expression could not be evaluated.
	 */
	public static Optional<Number> evaluate(Expression expr) {
		// Un-enclosing parenthesis
		if (expr instanceof EnclosedExpr) expr = ((EnclosedExpr) expr).getInner();
		final Expression unwrappedExpr = expr;
		// Only evaluating...
		return expr.toLiteralExpr().flatMap(ExpressionEvaluator::evaluate) // the literals themselves
				.or(() -> unwrappedExpr.toBinaryExpr().flatMap(ExpressionEvaluator::evaluate)) // binary operations
				.or(() -> unwrappedExpr.toUnaryExpr().flatMap(ExpressionEvaluator::evaluate)) // unary operations
				.or(() -> unwrappedExpr.toCastExpr().flatMap(ExpressionEvaluator::evaluate)); // cast operations
	}
}
