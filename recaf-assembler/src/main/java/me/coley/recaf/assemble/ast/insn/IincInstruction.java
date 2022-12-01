package me.coley.recaf.assemble.ast.insn;

import me.coley.recaf.assemble.ast.Descriptor;
import me.coley.recaf.assemble.ast.Named;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.VariableReference;

/**
 * Integer increment instruction.
 *
 * @author Matt Coley
 */
public class IincInstruction extends AbstractInstruction implements Named, Descriptor, VariableReference {
	private final String identifier;
	private final int increment;

	/**
	 * @param opcode
	 * 		Increment instruction opcode.
	 * @param identifier
	 * 		Variable identifier.
	 * @param increment
	 * 		Increment value.
	 */
	public IincInstruction(int opcode, String identifier, int increment) {
		super(opcode);
		this.identifier = identifier;
		this.increment = increment;
	}

	/**
	 * @return Increment value.
	 */
	public int getIncrement() {
		return increment;
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.IINC;
	}

	@Override
	public String getVariableIdentifier() {
		return getName();
	}

	@Override
	public String getVariableDescriptor() {
		return getDesc();
	}

	@Override
	public boolean isObjectDescriptorExplicitlyDeclared() {
		// Type is not object, only ever an int.
		return false;
	}

	@Override
	public OpType getVariableOperation() {
		return OpType.UPDATE;
	}

	@Override
	public String getDesc() {
		return "I";
	}

	@Override
	public String getName() {
		return identifier;
	}

	@Override
	public String print(PrintContext context) {
		return getOpcode() + " " + getEscapedVariableIdentifier() + " " + getIncrement();
	}
}
