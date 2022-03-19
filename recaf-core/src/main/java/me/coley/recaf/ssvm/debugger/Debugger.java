package me.coley.recaf.ssvm.debugger;

import dev.xdark.ssvm.VirtualMachine;
import me.coley.recaf.ssvm.asm.RecafOpcodes;

/**
 * Debugger implementation for SSVM.
 * 
 * @author xDark
 */
public final class Debugger {

	/**
	 * Deny all constructions.
	 */
	private Debugger() {
	}

	/**
	 * Enables debugger.
	 * Calling method multiple times has no effect.
	 *
	 * @param vm
	 * 		VM instance.
	 */
	public static void enable(VirtualMachine vm) {
		vm.getInterface().setProcessor(RecafOpcodes.BREAKPOINT, new BreakpointInstructionProcessor());
	}

	/**
	 * Disables debugger.
	 * Calling method multiple times has no effect.
	 *
	 * @param vm
	 * 		VM instance.
	 */
	public static void disable(VirtualMachine vm) {
		vm.getInterface().setProcessor(RecafOpcodes.BREAKPOINT, null);
	}
}
