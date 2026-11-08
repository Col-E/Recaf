package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.util.Set;

/**
 * Various handle utils.
 *
 * @author Matt Coley
 */
public class Handles {
	/**
	 * Handle used to bind single-abstract-methods (SAM) to an implementation.
	 */
	public static final Handle META_FACTORY = new Handle(
			Opcodes.H_INVOKESTATIC,
			"java/lang/invoke/LambdaMetafactory",
			"metafactory",
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" +
					"Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)" +
					"Ljava/lang/invoke/CallSite;", false);

	/**
	 * Handle used to bind serializable lambdas, lambdas with marker interfaces, and lambdas needing bridges.
	 */
	public static final Handle ALT_META_FACTORY = new Handle(
			Opcodes.H_INVOKESTATIC,
			"java/lang/invoke/LambdaMetafactory",
			"altMetafactory",
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" +
					"[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;", false);

	/**
	 * Handle used to bind a string pattern (with template placeholders) to a string concatenation implementation.
	 */
	public static final Handle STRING_CONCAT_FACTORY = new Handle(
			Opcodes.H_INVOKESTATIC,
			"java/lang/invoke/StringConcatFactory",
			"makeConcatWithConstants",
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" +
					"Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;", false);

	/**
	 * Handle used to bind string concatenation without constants to a string concatenation implementation.
	 */
	public static final Handle MAKE_CONCAT = new Handle(
			Opcodes.H_INVOKESTATIC,
			"java/lang/invoke/StringConcatFactory",
			"makeConcat",
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)" +
					"Ljava/lang/invoke/CallSite;", false);

	/**
	 * Handle used to bind record {@code toString}, {@code hashCode}, and {@code equals} implementations.
	 */
	public static final Handle OBJECT_METHODS = new Handle(
			Opcodes.H_INVOKESTATIC,
			"java/lang/runtime/ObjectMethods",
			"bootstrap",
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;" +
					"Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;", false);

	/**
	 * Handle used to bind pattern matching {@code switch} statements over reference types.
	 */
	public static final Handle TYPE_SWITCH = new Handle(
			Opcodes.H_INVOKESTATIC,
			"java/lang/runtime/SwitchBootstraps",
			"typeSwitch",
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" +
					"[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;", false);

	/**
	 * Handle used to bind pattern matching {@code switch} statements over enum types.
	 */
	public static final Handle ENUM_SWITCH = new Handle(
			Opcodes.H_INVOKESTATIC,
			"java/lang/runtime/SwitchBootstraps",
			"enumSwitch",
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" +
					"[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;", false);

	/**
	 * Handle used to load primitive class literals as dynamic constants.
	 */
	public static final Handle PRIMITIVE_CLASS = new Handle(
			Opcodes.H_INVOKESTATIC,
			"java/lang/invoke/ConstantBootstraps",
			"primitiveClass",
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Class;", false);

	/**
	 * Handle used to load {@code static final} field values as dynamic constants.
	 */
	public static final Handle GET_STATIC_FINAL = new Handle(
			Opcodes.H_INVOKESTATIC,
			"java/lang/invoke/ConstantBootstraps",
			"getStaticFinal",
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;", false);

	/**
	 * Handle used to compute values from a method handle as dynamic constants.
	 */
	public static final Handle CONSTANT_INVOKE = new Handle(
			Opcodes.H_INVOKESTATIC,
			"java/lang/invoke/ConstantBootstraps",
			"invoke",
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;" +
					"Ljava/lang/invoke/MethodHandle;[Ljava/lang/Object;)Ljava/lang/Object;", false);

	/**
	 * Bootstrap methods that {@code javac} emits for {@code invokedynamic} instructions and dynamic constants.
	 */
	private static final Set<Handle> JAVAC_BOOTSTRAPS = Set.of(
			META_FACTORY, ALT_META_FACTORY,
			STRING_CONCAT_FACTORY, MAKE_CONCAT,
			OBJECT_METHODS,
			TYPE_SWITCH, ENUM_SWITCH,
			PRIMITIVE_CLASS, GET_STATIC_FINAL, CONSTANT_INVOKE
	);

	/**
	 * @param bootstrap
	 * 		Bootstrap method handle of an {@code invokedynamic} instruction or dynamic constant.
	 *
	 * @return {@code true} when {@code javac} emits the bootstrap.
	 * {@code false} for any custom bootstrap method.
	 */
	public static boolean isJavacBootstrap(@Nonnull Handle bootstrap) {
		return JAVAC_BOOTSTRAPS.contains(bootstrap);
	}
}
