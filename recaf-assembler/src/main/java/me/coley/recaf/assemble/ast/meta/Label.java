package me.coley.recaf.assemble.ast.meta;

import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.Named;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.InstructionType;

/**
 * An abstraction of a code offset by ASM. Used by variables and other attributes to make
 * tracking applicable ranges more legible.
 *
 * @author Matt Coley
 */
public class Label extends AbstractInstruction implements Named {
	private final String name;

	/**
	 * @param name
	 * 		Label name
	 */
	public Label(String name) {
		super("LABEL", -1);
		this.name = name;
	}

	@Override
	public void insertInto(Code code) {
		code.addLabel(this);
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.LABEL;
	}

	@Override
	public String print(PrintContext context) {
		return name + ":";
	}

	@Override
	public String getName() {
		return name;
	}
}
