package me.coley.recaf;

import org.objectweb.asm.Opcodes;

/**
 * Wider-scale constants used by Recaf.
 *
 * @author Matt Coley
 */
public final class RecafConstants {
	private static final int ASM_VERSION = Opcodes.ASM9;
	private static final String URL_BUG_REPORT = "https://github.com/Col-E/Recaf/issues/" +
			"new?&template=bug_report.md";
	/**
	 * @return ASM version to use.
	 */
	public static int getAsmVersion() {
		return ASM_VERSION;
	}
	/**
	 * @return URL to open a new bug report.
	 */
	public static String getUrlBugReport() {
		return URL_BUG_REPORT;
	}
}
