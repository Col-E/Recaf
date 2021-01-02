package me.coley.recaf;

import org.objectweb.asm.Opcodes;

/**
 * Wider-scale constants used by Recaf.
 *
 * @author Matt Coley
 */
public final class RecafConstants {
	/**
	 * Latest released version.
	 */
	public static final String VERSION = Recaf.class.getPackage().getSpecificationVersion();
	/**
	 * ASM version to use.
	 */
	public static final int ASM_VERSION = Opcodes.ASM9;
}
