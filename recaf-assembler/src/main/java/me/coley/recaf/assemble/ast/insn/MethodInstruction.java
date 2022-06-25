package me.coley.recaf.assemble.ast.insn;

import me.coley.recaf.assemble.ast.PrintContext;

/**
 * Method reference instruction.
 *
 * @author Matt Coley
 */
public class MethodInstruction extends AbstractInstruction {
	private final String owner;
	private final String name;
	private final String desc;
	private final boolean itf;

	/**
	 * @param opcode
	 * 		Method instruction.
	 * @param owner
	 * 		Class declaring the method.
	 * @param name
	 * 		Name of the method.
	 * @param desc
	 * 		Type descriptor of the method.
	 * @param itf
	 * 		Interface method target flag.
	 */
	public MethodInstruction(int opcode, String owner, String name, String desc, boolean itf) {
		super(opcode);
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		this.itf = itf;
	}

	/**
	 * @return Class declaring the method.
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @return Name of the method.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Type descriptor of the method.
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * @return Interface method target flag.
	 */
	public boolean isItf() {
		return itf;
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.METHOD;
	}

	@Override
	public String print(PrintContext context) {
		// TODO: Handle edge case where 'itf=true' when opcode is not 'invokeinterface'
		//  - Jasm now has a custom instruction for it. Make sure we emit it in the disassembler properly.
		return getOpcode() + " " + getOwner() + "." + getName() + " " + getDesc();
	}
}
