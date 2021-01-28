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
	/**
	 * URL to open a new bug report.
	 */
	public static final String URL_BUG_REPORT = "https://github.com/Col-E/Recaf/issues/" +
			"new?&template=bug_report.md";
}
