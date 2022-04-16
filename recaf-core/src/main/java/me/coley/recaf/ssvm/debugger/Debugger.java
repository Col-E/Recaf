package me.coley.recaf.ssvm.debugger;

import dev.xdark.ssvm.VirtualMachine;
import me.coley.cafedude.classfile.instruction.ReservedOpcodes;

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
		vm.getInterface().setProcessor(ReservedOpcodes.breakpoint, new BreakpointInstructionProcessor());
	}

	/**
	 * Disables debugger.
	 * Calling method multiple times has no effect.
	 *
	 * @param vm
	 * 		VM instance.
	 */
	public static void disable(VirtualMachine vm) {
		vm.getInterface().setProcessor(ReservedOpcodes.breakpoint, null);
	}
}
