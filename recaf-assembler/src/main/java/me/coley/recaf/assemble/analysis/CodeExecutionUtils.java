package me.coley.recaf.assemble.analysis;

import me.coley.recaf.util.Types;
import org.objectweb.asm.Type;

import java.util.function.BiFunction;
import java.util.function.Function;

import static org.objectweb.asm.Type.*;

public class CodeExecutionUtils {
	public static void validateStackType(Frame frame, Type targetType, int offset) {
		String targetTypeName = targetType.getClassName();
		Value peek = frame.peek(offset);
		Type actualType;
		if (peek.isNull()) {
			actualType = Types.OBJECT_TYPE;
		} else if (peek.isObject()) {
			Value.ObjectValue objectValue = (Value.ObjectValue) peek;
			actualType = objectValue.getType();
		} else if (peek.isArray()) {
			Value.ArrayValue arrayValue = (Value.ArrayValue) peek;
			actualType = arrayValue.getArrayType();
		} else if (peek.isNumeric()) {
			Value.NumericValue numericValue = (Value.NumericValue) peek;
			actualType = numericValue.getType();
		} else if (peek.isWideReserved()) {
			frame.markWonky("Expected stack(top - " + offset + ") " +
					"to be " + targetTypeName + " but was wide-reserved value");
			return;
		} else if (peek.isEmptyStack()) {
			frame.markWonky("Expected stack(top - " + offset + ") " +
					"to be " + targetTypeName + " but was stack-underflow");
			return;
		} else {
			frame.markWonky("Expected stack(top - " + offset + ") " +
					"to be " + targetTypeName + " but was unknown value type: " + peek.getClass().getSimpleName());
			return;
		}
		int targetSort = targetType.getSort();
		switch (targetSort) {
			case BOOLEAN:
			case CHAR:
			case BYTE:
			case SHORT:
			case INT:
				// Allow for type widening on integers
				int actualSort = actualType.getSort();
				if (targetSort < actualSort) {
					frame.markWonky("Expected stack(top - " + offset + ") " +
							"to be " + targetTypeName + ", got " + actualType.getClassName());
				}
				break;
			case FLOAT:
			case LONG:
			case DOUBLE:
			case ARRAY:
			case OBJECT:
				if (targetType != actualType) {
					frame.markWonky("Expected stack(top - " + offset + ") " +
							"to be " + targetTypeName + ", got " + actualType.getClassName());
				}
				break;
			default:
				throw new IllegalArgumentException("Illegal target type: " + targetType);
		}
	}

	public static void binaryOp(Frame frame, Type type, BiFunction<Number, Number, Number> function) {
		Value value1 = frame.pop();
		Value value2 = frame.pop();
		if (value1 instanceof Value.NumericValue && value2 instanceof Value.NumericValue)
			evaluateMathOp(frame, type, function, (Value.NumericValue) value2, (Value.NumericValue) value1);
		else {
			frame.markWonky("One or both math operands on stack are non-numeric");
			frame.push(new Value.NumericValue(type));
		}
	}

	public static void binaryOpWide(boolean rightIsWide, Frame frame, Type type, BiFunction<Number, Number, Number> function) {
		Value value1 = rightIsWide ? frame.popWide() : frame.pop();
		Value value2 = frame.popWide();
		if (value1 instanceof Value.NumericValue && value2 instanceof Value.NumericValue)
			evaluateMathOp(frame, type, function, (Value.NumericValue) value2, (Value.NumericValue) value1);
		else {
			frame.markWonky("One or both math operands on stack are non-numeric");
			pushValue(frame, type, new Value.NumericValue(type));
		}
	}

	public static void binaryOpWide(Frame frame, Type type, BiFunction<Number, Number, Number> function) {
		binaryOpWide(true, frame, type, function);
	}

	public static void evaluateMathOp(Frame frame, Type type, BiFunction<Number, Number, Number> function, Value.NumericValue value2, Value.NumericValue value1) {
		Value.NumericValue result;
		try {
			Number arg1 = value1.getNumber();
			Number arg2 = value2.getNumber();
			if (arg1 == null || arg2 == null)
				result = new Value.NumericValue(type);
			else
				result = new Value.NumericValue(type, function.apply(arg1, arg2));
		} catch (Exception ex) {
			result = new Value.NumericValue(type);
			frame.markWonky("One or both math operands on stack are non-numeric");
		}
		pushValue(frame, type, result);
	}

	public static void unaryOp(Frame frame, Type type, Function<Number, Number> function) {
		Value value = frame.pop();
		if (value instanceof Value.NumericValue)
			evaluateUnaryOp(frame, type, function, (Value.NumericValue) value);
		else {
			frame.markWonky("Math operand on stack is non-numeric");
			pushValue(frame, type, new Value.NumericValue(type));
		}
	}

	public static void unaryOpWide(Frame frame, Type type, Function<Number, Number> function) {
		Value value = frame.popWide();
		if (value instanceof Value.NumericValue)
			evaluateUnaryOp(frame, type, function, (Value.NumericValue) value);
		else {
			frame.markWonky("Math operand on stack is non-numeric");
			pushValue(frame, type, new Value.NumericValue(type));
		}
	}

	public static void evaluateUnaryOp(Frame frame, Type type, Function<Number, Number> function, Value.NumericValue value) {
		Value.NumericValue result;
		try {
			Number arg = value.getNumber();
			if (arg == null)
				result = new Value.NumericValue(type);
			else
				result = new Value.NumericValue(type, function.apply(arg));
		} catch (Exception ex) {
			result = new Value.NumericValue(type);
			frame.markWonky("Math operand on stack is non-numeric");
		}
		pushValue(frame, type, result);
	}

	public static void pushValue(Frame frame, Type type, Value result) {
		frame.push(result);
		if (Types.isWide(type))
			frame.push(new Value.WideReservedValue());
	}
}
