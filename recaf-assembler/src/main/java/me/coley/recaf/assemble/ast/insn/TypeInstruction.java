package me.coley.recaf.assemble.ast.insn;

import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.util.EscapeUtil;

/**
 * Type instruction.
 *
 * @author Matt Coley
 */
public class TypeInstruction extends AbstractInstruction {
	private final String type;

	/**
	 * @param opcode
	 * 		Type instruction opcode.
	 * @param type
	 * 		Type descriptor.
	 */
	public TypeInstruction(int opcode, String type) {
		super(opcode);
		this.type = type;
	}

	/**
	 * @return Type descriptor.
	 */
	public String getType() {
		return type;
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.TYPE;
	}

	@Override
	public String print(PrintContext context) {
		return String.format("%s %s", getOpcode(), context.fmtIdentifier(getType()));
	}
}
