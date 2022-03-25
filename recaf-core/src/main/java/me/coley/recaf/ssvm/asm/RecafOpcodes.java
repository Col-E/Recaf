package me.coley.recaf.ssvm.asm;

/**
 * Recaf-specific opcodes for SSVM.
 * 
 * @author xDark
 */
public class RecafOpcodes {
	/**
	 * <a href="https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-6.html#jvms-6.2">VM 6.2</a> declares that
	 * debuggers should use 202 <i>(0xCA)</i> as a breakpoint.
	 */
	public static final int BREAKPOINT = 0xCA;
}
