package software.coley.recaf;

import org.objectweb.asm.Opcodes;

/**
 * Common constants.
 *
 * @author Matt Coley
 */
public final class RecafConstants {
	private RecafConstants() {
	}

	/**
	 * @return Current ASM version.
	 */
	public static int getAsmVersion() {
		return Opcodes.ASM9;
	}
}
