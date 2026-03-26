package software.coley.recaf.services.callgraph;

import me.darknet.dex.tree.definitions.instructions.Invoke;
import org.objectweb.asm.Opcodes;

/**
 * Common invoke-kind model used by all supported class formats.
 *
 * @author Matt Coley
 */
public enum InvokeKind {
	/** Method is invoked directly, without virtual dispatch. */
	DIRECT,
	/** Method is invoked directly, without virtual dispatch. */
	SPECIAL,
	/** Method is invoked via an interface. */
	INTERFACE,
	/** Method is invoked via static dispatch. */
	STATIC,
	/** Method is invoked via a super call <i>(Android specific)</i>. */
	SUPER,
	/** Method is invoked via virtual dispatch. */
	VIRTUAL;

	/**
	 * @param opcode
	 * 		ASM opcode or handle tag.
	 *
	 * @return Invoke kind.
	 */
	public static InvokeKind fromJvmOpcode(int opcode) {
		return switch (opcode) {
			case Opcodes.H_INVOKESPECIAL, Opcodes.INVOKESPECIAL -> SPECIAL;
			case Opcodes.H_INVOKEVIRTUAL, Opcodes.INVOKEVIRTUAL -> VIRTUAL;
			case Opcodes.H_INVOKESTATIC, Opcodes.INVOKESTATIC -> STATIC;
			case Opcodes.H_INVOKEINTERFACE, Opcodes.INVOKEINTERFACE -> INTERFACE;
			default -> throw new IllegalArgumentException("Unsupported JVM invoke opcode: " + opcode);
		};
	}

	/**
	 * @param opcode
	 * 		Dex invoke opcode.
	 *
	 * @return Invoke kind.
	 */
	public static InvokeKind fromDexOpcode(int opcode) {
		return switch (opcode) {
			case Invoke.DIRECT -> DIRECT;
			case Invoke.STATIC -> STATIC;
			case Invoke.VIRTUAL, Invoke.POLYMORPHIC -> VIRTUAL;
			case Invoke.INTERFACE -> INTERFACE;
			case Invoke.SUPER -> SUPER;
			default -> throw new IllegalArgumentException("Unsupported dex invoke opcode: " + opcode);
		};
	}
}
