package software.coley.recaf.util;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

/**
 * Various handle utils.
 *
 * @author Matt Coley
 */
public class Handles {
	public static final Handle META_FACTORY = new Handle(
			Opcodes.H_INVOKESTATIC,
			"java/lang/invoke/LambdaMetafactory",
			"metafactory",
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" +
					"Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)" +
					"Ljava/lang/invoke/CallSite;", false);

}
