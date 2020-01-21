package me.coley.recaf.simulation;

final class ASMInstructionHandlers {
	private static final InstructionHandler[] HANDLERS;

	private ASMInstructionHandlers() { }

	static InstructionHandler getHandlerForOpcode(int opcode) {
		return HANDLERS[opcode];
	}

	static {
		InstructionHandler[] handlers = new InstructionHandler[199];
		HANDLERS = handlers;
	}
}
