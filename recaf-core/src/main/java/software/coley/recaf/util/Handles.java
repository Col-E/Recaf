package software.coley.recaf.util;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

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
	 * Handle used to bind a string pattern (with template placeholders) to a string concatenation implementation.
	 */
	public static final Handle STRING_CONCAT_FACTORY = new Handle(
			Opcodes.H_INVOKESTATIC,
			"java/lang/invoke/StringConcatFactory",
			"makeConcatWithConstants",
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" +
					"Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;", false);

}
