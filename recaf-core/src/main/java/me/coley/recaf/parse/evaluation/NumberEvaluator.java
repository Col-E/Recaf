package me.coley.recaf.parse.evaluation;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.type.PrimitiveType;

import java.util.Optional;

/**
 * Utilities to evaluate {@link com.github.javaparser.ast.expr.BinaryExpr.Operator} and
 * {@link com.github.javaparser.ast.expr.UnaryExpr.Operator} values.
 *
 * @author Amejonah
 */
public class NumberEvaluator {
	private NumberEvaluator() {}

	/**
	 * @param op
	 * 		Math operation.
	 * @param a
	 * 		Left number to operate with.
	 * @param b
	 * 		Right number to operate with.
	 *
	 * @return Number value of the operation.
	 */
	public static Optional<Number> evaluate(Number a, BinaryExpr.Operator op, Number b) {
		// it does not change anything if the type is shorter than a long
		// therefore, we can use longs for the right hand side
		if (op == BinaryExpr.Operator.LEFT_SHIFT ||
				op == BinaryExpr.Operator.SIGNED_RIGHT_SHIFT ||
				op == BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT) {
			return evaluateShift(a, op, b.longValue());
		}
		if (a instanceof Double || b instanceof Double) {
			return evaluate(a.doubleValue(), op, b.doubleValue());
		} else if (a instanceof Float || b instanceof Float) {
			return evaluate(a.floatValue(), op, b.floatValue());
		}
		if (a instanceof Long || b instanceof Long) {
			return evaluate(a.longValue(), op, b.longValue());
		} else if ((a instanceof Byte || a instanceof Short || a instanceof Integer)
				&& (b instanceof Byte || b instanceof Short || b instanceof Integer)) {
			return evaluate(a.intValue(), op, b.intValue());
		} else {
			return Optional.empty();
		}
	}

	/**
	 * @param op
	 * 		Math operation.
	 * @param a
	 * 		Left number to operate with.
	 * @param b
	 * 		Right number to operate with.
	 *
	 * @return Number value of the operation.
	 */
	public static Optional<Number> evaluate(long a, BinaryExpr.Operator op, long b) {
		switch (op) {
			case BINARY_OR: return Optional.of(a | b);
			case BINARY_AND: return Optional.of(a & b);
			case XOR: return Optional.of(a ^ b);
			case PLUS: return Optional.of(a + b);
			case MINUS: return Optional.of(a - b);
			case MULTIPLY: return Optional.of(a * b);
			case DIVIDE: return Optional.of(a / b);
			case REMAINDER: return Optional.of(a % b);
			default:
		}
		return Optional.empty();
	}

	/**
	 * @param op
	 * 		Math operation.
	 * @param a
	 * 		Left number to operate with.
	 * @param b
	 * 		Right number to operate with.
	 *
	 * @return Number value of the operation.
	 */
	public static Optional<Number> evaluate(int a, BinaryExpr.Operator op, int b) {
		switch (op) {
			case BINARY_OR: return Optional.of(a | b);
			case BINARY_AND: return Optional.of(a & b);
			case XOR: return Optional.of(a ^ b);
			case PLUS: return Optional.of(a + b);
			case MINUS: return Optional.of(a - b);
			case MULTIPLY: return Optional.of(a * b);
			case DIVIDE: return Optional.of(a / b);
			case REMAINDER: return Optional.of(a % b);
			default:
		}
		return Optional.empty();
	}

	/**
	 * @param op
	 * 		Math operation.
	 * @param a
	 * 		Left number to operate with.
	 * @param b
	 * 		Right number to operate with.
	 *
	 * @return Number value of the operation.
	 */
	public static Optional<Number> evaluate(double a, BinaryExpr.Operator op, double b) {
		switch (op) {
			case PLUS: return Optional.of(a + b);
			case MINUS: return Optional.of(a - b);
			case MULTIPLY: return Optional.of(a * b);
			case DIVIDE: return Optional.of(a / b);
			case REMAINDER: return Optional.of(a % b);
			default:
		}
		return Optional.empty();
	}

	/**
	 * @param op
	 * 		Math operation.
	 * @param a
	 * 		Left number to operate with.
	 * @param b
	 * 		Right number to operate with.
	 *
	 * @return Number value of the operation.
	 */
	public static Optional<Number> evaluate(float a, BinaryExpr.Operator op, float b) {
		switch (op) {
			case PLUS: return Optional.of(a + b);
			case MINUS: return Optional.of(a - b);
			case MULTIPLY: return Optional.of(a * b);
			case DIVIDE: return Optional.of(a / b);
			case REMAINDER: return Optional.of(a % b);
			default:
		}
		return Optional.empty();
	}

	/**
	 * @param op
	 * 		Math operation.
	 * @param n
	 * 		Number to operate on.
	 *
	 * @return Number value with the operation applied.
	 */
	public static Optional<Number> evaluate(UnaryExpr.Operator op, Number n) {
		switch (op) {
			case PLUS: return Optional.of(n);
			case BITWISE_COMPLEMENT:
				if(n instanceof Byte || n instanceof Short || n instanceof Integer) return Optional.of(~n.intValue());
				if (n instanceof Long) return Optional.of(~n.longValue());
				return Optional.empty();
			case MINUS:
				if(n instanceof Byte || n instanceof Short || n instanceof Integer) return Optional.of(-n.intValue());
				if (n instanceof Long) return Optional.of(-n.longValue());
				if (n instanceof Float) return Optional.of(-n.floatValue());
				if (n instanceof Double) return Optional.of(-n.doubleValue());
			default: return Optional.empty();
		}
	}

	/**
	 * @param n
	 * 		Number to shift.
	 * @param op
	 * 		Shift operation.
	 * @param b
	 * 		Value to shift by.
	 *
	 * @return Number value shifted.
	 */
	public static Optional<Number> evaluateShift(Number n, BinaryExpr.Operator op, long b) {
		switch (op) {
			case LEFT_SHIFT:
				if(n instanceof Long) return Optional.of(n.longValue() << b);
				if(n instanceof Byte || n instanceof Short || n instanceof Integer) return Optional.of(n.intValue() << b);
				break;
			case SIGNED_RIGHT_SHIFT:
				if(n instanceof Long) return Optional.of(n.longValue() >> b);
				if(n instanceof Byte || n instanceof Short || n instanceof Integer) return Optional.of(n.intValue() >> b);
				break;
			case UNSIGNED_RIGHT_SHIFT:
				if(n instanceof Long) return Optional.of(n.longValue() >>> b);
				if(n instanceof Byte || n instanceof Short || n instanceof Integer) return Optional.of(n.intValue() >>> b);
			default:
		}
		return Optional.empty();
	}

	/**
	 * @param n
	 * 		Number to cast.
	 * @param type
	 * 		Target type.
	 *
	 * @return Number value when cast to the given type.
	 */
	public static Optional<Number> cast(Number n, PrimitiveType type) {
		return type.toPrimitiveType().flatMap(t -> {
			switch (t.getType()) {
				case BYTE: return Optional.of(n.byteValue());
				case SHORT: return Optional.of(n.shortValue());
				case INT: return Optional.of(n.intValue());
				case LONG: return Optional.of(n.longValue());
				case FLOAT: return Optional.of(n.floatValue());
				case DOUBLE: return Optional.of(n.doubleValue());
			}
			return Optional.empty();
		});
	}
}
