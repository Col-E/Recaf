package me.coley.recaf.evaluation;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;

import java.util.Optional;

public class NumberEvaluator {

	private NumberEvaluator() {}

	public static Optional<Number> evaluate(Number a, BinaryExpr.Operator op, Number b) {
		if (a instanceof Integer && b instanceof Integer) {
			return evaluate(a.intValue(), op, b.intValue());
		} else if (a instanceof Long && b instanceof Long) {
			return evaluate(a.longValue(), op, b.longValue());
		} else if (a instanceof Integer && b instanceof Long) {
			return evaluate(a.intValue(), op, b.longValue());
		} else if (a instanceof Long && b instanceof Integer) {
			return evaluate(a.longValue(), op, b.intValue());
		} else if (a instanceof Double || b instanceof Double) {
			return evaluate(a.doubleValue(), op, b.doubleValue());
		} else if (a instanceof Float && b instanceof Float) {
			return evaluate(a.floatValue(), op, b.floatValue());
		} else {
			return Optional.empty();
		}
	}

	public static Optional<Number> higher(Number n) {
		if (n instanceof Integer) return Optional.of(n.longValue());
		if (n instanceof Float) return Optional.of(n.doubleValue());
		return Optional.ofNullable(n);
	}

	public static Optional<Number> evaluate(long a, BinaryExpr.Operator op, long b) {
		switch (op) {
			case BINARY_OR:
				return Optional.of(a | b);
			case BINARY_AND:
				return Optional.of(a & b);
			case XOR:
				return Optional.of(a ^ b);
			case LEFT_SHIFT:
				return Optional.of(a << b);
			case SIGNED_RIGHT_SHIFT:
				return Optional.of(a >> b);
			case UNSIGNED_RIGHT_SHIFT:
				return Optional.of(a >>> b);
			case PLUS:
				return Optional.of(a + b);
			case MINUS:
				return Optional.of(a - b);
			case MULTIPLY:
				return Optional.of(a * b);
			case DIVIDE:
				return Optional.of(a / b);
			case REMAINDER:
				return Optional.of(a % b);
			default:
		}
		return Optional.empty();
	}

	public static Optional<Number> evaluate(int a, BinaryExpr.Operator op, int b) {
		switch (op) {
			case BINARY_OR:
				return Optional.of(a | b);
			case BINARY_AND:
				return Optional.of(a & b);
			case XOR:
				return Optional.of(a ^ b);
			case LEFT_SHIFT:
				return Optional.of(a << b);
			case SIGNED_RIGHT_SHIFT:
				return Optional.of(a >> b);
			case UNSIGNED_RIGHT_SHIFT:
				return Optional.of(a >>> b);
			case PLUS:
				return Optional.of(a + b);
			case MINUS:
				return Optional.of(a - b);
			case MULTIPLY:
				return Optional.of(a * b);
			case DIVIDE:
				return Optional.of(a / b);
			case REMAINDER:
				return Optional.of(a % b);
			default:
		}
		return Optional.empty();
	}

	public static Optional<Number> evaluate(long a, BinaryExpr.Operator op, int b) {
		return evaluate(a, op, (long) b);
	}

	public static Optional<Number> evaluate(int a, BinaryExpr.Operator op, long b) {
		switch (op) {
			case SIGNED_RIGHT_SHIFT:
				return Optional.of(a >> b);
			case UNSIGNED_RIGHT_SHIFT:
				return Optional.of(a >>> b);
			case LEFT_SHIFT:
				return Optional.of(a << b);
			case PLUS:
			case MINUS:
			case MULTIPLY:
			case DIVIDE:
			case REMAINDER:
			case BINARY_OR:
			case BINARY_AND:
			case XOR:
				return evaluate((long) a, op, b);
			default:
		}
		return Optional.empty();
	}

	public static Optional<Number> evaluate(double a, BinaryExpr.Operator op, double b) {
		switch (op) {
			case PLUS:
				return Optional.of(a + b);
			case MINUS:
				return Optional.of(a - b);
			case MULTIPLY:
				return Optional.of(a * b);
			case DIVIDE:
				return Optional.of(a / b);
			case REMAINDER:
				return Optional.of(a % b);
			default:
		}
		return Optional.empty();
	}

	public static Optional<Number> evaluate(float a, BinaryExpr.Operator op, double b) {
		return evaluate((double) a, op, b);
	}

	public static Optional<Number> evaluate(double a, BinaryExpr.Operator op, float b) {
		return evaluate(a, op, (double) b);
	}

	public static Optional<Number> evaluate(float a, BinaryExpr.Operator op, float b) {
		switch (op) {
			case PLUS:
				return Optional.of(a + b);
			case MINUS:
				return Optional.of(a - b);
			case MULTIPLY:
				return Optional.of(a * b);
			case DIVIDE:
				return Optional.of(a / b);
			case REMAINDER:
				return Optional.of(a % b);
			default:
		}
		return Optional.empty();
	}

	public static Optional<Number> evaluate(UnaryExpr.Operator operator, Number n) {
		switch (operator) {
			case PLUS:
				return Optional.of(n);
			case BITWISE_COMPLEMENT:
				if (n instanceof Integer) return Optional.of(~n.intValue());
				if (n instanceof Long) return Optional.of(~n.longValue());
				return Optional.empty();
			case MINUS:
				if (n instanceof Integer) return Optional.of(-n.intValue());
				if (n instanceof Long) return Optional.of(-n.longValue());
				if (n instanceof Float) return Optional.of(-n.floatValue());
				if (n instanceof Double) return Optional.of(-n.doubleValue());
			default:
				return Optional.empty();
		}
	}

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

	public static Optional<Number> cast(Number n, Type type) {
		return type.toPrimitiveType().flatMap(t -> cast(n, t));
	}
}
