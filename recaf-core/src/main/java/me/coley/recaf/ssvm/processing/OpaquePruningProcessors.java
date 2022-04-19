package me.coley.recaf.ssvm.processing;

import dev.xdark.ssvm.VirtualMachine;

public class OpaquePruningProcessors {
	public static void install(VirtualMachine vm) {
		// TODO: If a flow path's arguments are all constants, just delete the other flow-path's contents
		//       since it indicates an opaque predicate. This should be done in a separate processor.
		//       This one should be updated to not take the never-true path.
	}
}
