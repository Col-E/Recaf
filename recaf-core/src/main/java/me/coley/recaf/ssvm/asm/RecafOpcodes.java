package me.coley.recaf.ssvm.asm;

import dev.xdark.ssvm.asm.VMOpcodes;

/**
 * Recaf-specific opcodes for SSVM.
 * 
 * @author xDark
 */
public class RecafOpcodes {
	
	public static final int BREAKPOINT = VMOpcodes.LDC + 1;
}
