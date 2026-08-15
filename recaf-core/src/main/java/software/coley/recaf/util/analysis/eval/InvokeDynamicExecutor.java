package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import software.coley.recaf.util.Handles;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.value.DoubleValue;
import software.coley.recaf.util.analysis.value.FloatValue;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;
import software.coley.recaf.util.analysis.value.impl.ObjectValueImpl;

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
		return isSupportedStringConcat(indy) || isSupportedLambda(indy);
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
		if (isSupportedLambda(indy) && values.size() == Type.getArgumentCount(indy.desc))
			return new EvaluatedLambdaValue(
					Type.getReturnType(indy.desc),
					indy.name,
					(Type) indy.bsmArgs[0],
					(Type) indy.bsmArgs[2],
					(Handle) indy.bsmArgs[1],
					values);
		return null;
	}

	/**
	 * Checks whether a functional-interface invocation is backed by a supported lambda in the given scope.
	 *
	 * @param min
	 * 		Method invocation to check.
	 * @param instructionScope
	 * 		Method or block instructions containing the invocation.
	 *
	 * @return {@code true} if a matching supported lambda creation exists in the scope.
	 */
	public static boolean canEvaluateLambdaInvocation(@Nonnull MethodInsnNode min, @Nonnull InsnList instructionScope) {
		for (AbstractInsnNode instruction : instructionScope) {
			// Continue until we find a supported lambda creation that matches the functional interface method invocation.
			if (!(instruction instanceof InvokeDynamicInsnNode indy) || !isSupportedLambda(indy))
				continue;

			// Check if the functional interface type, method name, and method descriptor match the invocation.
			Type functionalInterfaceType = Type.getReturnType(indy.desc);
			Type samMethodType = (Type) indy.bsmArgs[0];
			if (functionalInterfaceType.getInternalName().equals(min.owner)
					&& indy.name.equals(min.name)
					&& samMethodType.getDescriptor().equals(min.desc))
				return true;
		}
		return false;
	}

	/**
	 * Checks whether an instruction represents a supported standard lambda creation.
	 *
	 * @param indy
	 * 		Instruction to check.
	 *
	 * @return {@code true} if the instruction represents a supported lambda creation, {@code false} otherwise.
	 */
	private static boolean isSupportedLambda(@Nonnull InvokeDynamicInsnNode indy) {
		// Example of what we're looking for:
		//
		// invokedynamic apply:()Ljava/util/function/Function;
		//  arg[0]: (Ljava/lang/Object;)Ljava/lang/Object;
		//  arg[1]: H_INVOKESTATIC Example.lambda$apply$0(Ljava/lang/String;)Ljava/lang/String;
		//  arg[2]: (Ljava/lang/String;)Ljava/lang/String;
		if (!Handles.META_FACTORY.equals(indy.bsm)
				|| Type.getReturnType(indy.desc).getSort() != Type.OBJECT
				|| indy.bsmArgs == null
				|| indy.bsmArgs.length != 3
				|| !(indy.bsmArgs[0] instanceof Type samMethodType)
				|| !(indy.bsmArgs[1] instanceof Handle implementationHandle)
				|| !(indy.bsmArgs[2] instanceof Type instantiatedMethodType)
				|| samMethodType.getSort() != Type.METHOD
				|| instantiatedMethodType.getSort() != Type.METHOD)
			return false;

		// We have a matching lambda creation, but we need to check that the implementation
		// method matches the shape of the functional interface method.
		//  - Must be static
		//  - Must have same number of args, plus the number of captured values
		//  - Must have same return type
		try {
			Type[] samArguments = samMethodType.getArgumentTypes();
			Type[] instantiatedArguments = instantiatedMethodType.getArgumentTypes();
			Type implementationMethodType = Type.getMethodType(implementationHandle.getDesc());
			boolean samVoid = samMethodType.getReturnType() == Type.VOID_TYPE;
			boolean instantiatedVoid = instantiatedMethodType.getReturnType() == Type.VOID_TYPE;
			boolean implementationVoid = implementationMethodType.getReturnType() == Type.VOID_TYPE;
			return samArguments.length == instantiatedArguments.length
					&& samVoid == instantiatedVoid
					&& instantiatedVoid == implementationVoid
					&& implementationHandle.getTag() == Opcodes.H_INVOKESTATIC
					&& implementationMethodType.getArgumentCount() == Type.getArgumentCount(indy.desc) + instantiatedArguments.length;
		} catch (IllegalArgumentException ex) {
			// Malformed method descriptor, so we cannot evaluate this lambda creation.
			return false;
		}
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

		// Skip if the number of values does not match the number of arguments in the descriptor.
		Type[] argumentTypes = Type.getArgumentTypes(indy.desc);
		if (values.size() != argumentTypes.length)
			return null;

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
			if (indy.bsmArgs == null || indy.bsmArgs.length == 0 || !(indy.bsmArgs[0] instanceof String recipe))
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

	/**
	 * Host-independent value representing a modeled lambda instance.
	 */
	public static final class EvaluatedLambdaValue extends ObjectValueImpl {
		private final String samMethodName;
		private final Type samMethodType;
		private final Type instantiatedMethodType;
		private final Handle implementationHandle;
		private final List<ReValue> capturedValues;

		/**
		 * @param functionalInterfaceType
		 * 		Functional-interface type returned by the lambda creation.
		 * @param samMethodName
		 * 		Name of the single abstract method.
		 * @param samMethodType
		 * 		Erased single abstract method type.
		 * @param instantiatedMethodType
		 * 		Instantiated single abstract method type.
		 * @param implementationHandle
		 * 		Static implementation method handle.
		 * @param capturedValues
		 * 		Values captured by the lambda creation.
		 */
		EvaluatedLambdaValue(@Nonnull Type functionalInterfaceType, @Nonnull String samMethodName,
		                     @Nonnull Type samMethodType, @Nonnull Type instantiatedMethodType,
		                     @Nonnull Handle implementationHandle, @Nonnull List<ReValue> capturedValues) {
			super(functionalInterfaceType, Nullness.NOT_NULL);
			this.samMethodName = samMethodName;
			this.samMethodType = samMethodType;
			this.instantiatedMethodType = instantiatedMethodType;
			this.implementationHandle = implementationHandle;
			this.capturedValues = List.copyOf(capturedValues);
		}

		/**
		 * @return Always {@code true} since a modeled lambda is a known, non-null value.
		 */
		@Override
		public boolean hasKnownValue() {
			return true;
		}

		/**
		 * @return Name of the single abstract method.
		 */
		@Nonnull
		String samMethodName() {
			return samMethodName;
		}

		/**
		 * @return Erased single abstract method type.
		 */
		@Nonnull
		Type samMethodType() {
			return samMethodType;
		}

		/**
		 * @return Instantiated single abstract method type.
		 */
		@Nonnull
		Type instantiatedMethodType() {
			return instantiatedMethodType;
		}

		/**
		 * @return Static implementation method handle.
		 */
		@Nonnull
		Handle implementationHandle() {
			return implementationHandle;
		}

		/**
		 * @return Immutable captured evaluator values.
		 */
		@Nonnull
		List<ReValue> capturedValues() {
			return capturedValues;
		}

		/**
		 * Checks whether a method invocation targets this lambda's single abstract method.
		 *
		 * @param min
		 * 		Method invocation to check.
		 *
		 * @return {@code true} if the invocation matches this lambda's functional-interface method.
		 */
		boolean supportsInvocation(@Nonnull MethodInsnNode min) {
			return type().getInternalName().equals(min.owner)
					&& samMethodName.equals(min.name)
					&& samMethodType.getDescriptor().equals(min.desc);
		}
	}
}
