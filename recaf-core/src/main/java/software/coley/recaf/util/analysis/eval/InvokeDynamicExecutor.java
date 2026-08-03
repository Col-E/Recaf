package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import software.coley.recaf.util.analysis.value.DoubleValue;
import software.coley.recaf.util.analysis.value.FloatValue;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;

import java.util.List;

/**
 * Dispatches modeled {@code INVOKEDYNAMIC} execution.
 *
 * @author Matt Coley
 * @see Evaluator
 */
public class InvokeDynamicExecutor {
	private InvokeDynamicExecutor() {}

	/**
	 * Checks whether an invokedynamic instruction has a modeled handler.
	 *
	 * @param indy
	 * 		Instruction to check.
	 *
	 * @return {@code true} if the instruction can be evaluated by this helper.
	 */
	public static boolean canEvaluate(@Nonnull InvokeDynamicInsnNode indy) {
		return isSupportedStringConcat(indy);
	}

	/**
	 * Evaluates an invokedynamic instruction when a modeled handler supports it.
	 *
	 * @param indy
	 * 		Instruction to evaluate.
	 * @param values
	 * 		Operand values in descriptor order.
	 *
	 * @return Modeled result, or {@code null} when no handler can execute the instruction.
	 */
	@Nullable
	public static ReValue evaluate(@Nonnull InvokeDynamicInsnNode indy, @Nonnull List<ReValue> values) {
		if (isSupportedStringConcat(indy))
			return evaluateStringConcat(indy, values);
		return null;
	}

	/**
	 * Checks whether an instruction represents a supported string concatenation operation.
	 *
	 * @param indy
	 * 		Instruction to check.
	 *
	 * @return {@code true} if the instruction represents a supported string concatenation operation, {@code false} otherwise.
	 */
	private static boolean isSupportedStringConcat(@Nonnull InvokeDynamicInsnNode indy) {
		String owner = indy.bsm.getOwner();
		String name = indy.bsm.getName();
		Type returnType = Type.getReturnType(indy.desc);
		if (returnType.getSort() != Type.OBJECT || !returnType.getInternalName().equals("java/lang/String"))
			return false;
		if (!owner.equals("java/lang/invoke/StringConcatFactory"))
			return false;
		return name.equals("makeConcat") || name.equals("makeConcatWithConstants");
	}

	/**
	 * Evaluates a string concatenation operation represented by an {@link InvokeDynamicInsnNode}.
	 *
	 * @param indy
	 * 		Instruction representing the string concatenation.
	 * @param values
	 * 		Values to be concatenated, in the order they appear in the instruction.
	 *
	 * @return Formatted {@link StringValue} if the concatenation can be evaluated, or {@code null} if it cannot be evaluated.
	 */
	@Nullable
	private static ReValue evaluateStringConcat(@Nonnull InvokeDynamicInsnNode indy, @Nonnull List<ReValue> values) {
		if (!isSupportedStringConcat(indy))
			return null;

		Type[] argumentTypes = Type.getArgumentTypes(indy.desc);
		StringBuilder text = new StringBuilder();
		String name = indy.bsm.getName();
		if (name.equals("makeConcat")) {
			// Simple concatenation
			for (int i = 0; i < values.size(); i++) {
				String valueText = valueToString(values.get(i), argumentTypes[i]);
				if (valueText == null)
					return null;
				text.append(valueText);
			}
		} else if (name.equals("makeConcatWithConstants")) {
			// Must have a recipe string as the first argument, followed by constant arguments.
			if (indy.bsmArgs.length == 0 || !(indy.bsmArgs[0] instanceof String recipe))
				return null;

			// The recipe string uses '\u0001' as a placeholder for values and '\u0002' as a placeholder for constants.
			int valueIndex = 0;
			int constantIndex = 1;
			for (int i = 0; i < recipe.length(); i++) {
				char c = recipe.charAt(i);
				if (c == '\u0001') {
					if (valueIndex >= values.size())
						return null;
					String valueText = valueToString(values.get(valueIndex), argumentTypes[valueIndex]);
					if (valueText == null)
						return null;
					text.append(valueText);
					valueIndex++;
				} else if (c == '\u0002') {
					if (constantIndex >= indy.bsmArgs.length)
						return null;
					text.append(indy.bsmArgs[constantIndex++]);
				} else {
					text.append(c);
				}
			}
		} else {
			// Unsupported string concatenation method.
			return null;
		}

		return ObjectValue.string(text.toString());
	}

	/**
	 * Converts a {@link ReValue} to a string representation, if possible.
	 *
	 * @param value
	 * 		The value to convert.
	 * @param type
	 * 		The type of the value. Used to differentiate primitives that fit into the int space.
	 *
	 * @return The string representation of the value, or {@code null} if it cannot be converted.
	 */
	@Nullable
	private static String valueToString(@Nonnull ReValue value, @Nonnull Type type) {
		return switch (value) {
			case StringValue stringValue -> stringValue.getText().orElse(null);
			case ObjectValue objectValue when objectValue.isNull() -> "null";
			case InstancedObjectValue<?> instancedObjectValue when instancedObjectValue.getRealInstance() != null ->
					String.valueOf(instancedObjectValue.getRealInstance());
			case IntValue intValue when intValue.value().isPresent() -> {
				int i = intValue.value().getAsInt();
				yield switch (type.getSort()) {
					case Type.BOOLEAN -> String.valueOf(i != 0);
					case Type.CHAR -> String.valueOf((char) i);
					default -> String.valueOf(i);
				};
			}
			case LongValue longValue when longValue.value().isPresent() ->
					String.valueOf(longValue.value().getAsLong());
			case FloatValue floatValue when floatValue.value().isPresent() ->
					String.valueOf((float) floatValue.value().getAsDouble());
			case DoubleValue doubleValue when doubleValue.value().isPresent() ->
					String.valueOf(doubleValue.value().getAsDouble());
			default -> null;
		};
	}
}
