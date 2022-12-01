package me.coley.recaf.assemble.ast.insn;

import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.util.EscapeUtil;

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
		// Jasm has a custom format for normal 'invokex' instructions but with the abstracted away flag 'itf' set to
		// true, that's why we have to use the format 'invokexinterface' to tell jasm to use these special instructions.
		// we check for op != 'invokeinterface' because invokeinterface is always itf=true
		String opcode = getOpcode();
		if (isItf() && !opcode.equals("invokeinterface")) {
			opcode = opcode + "interface";
		}
		return opcode + " " +
				context.fmtIdentifier(getOwner()) + '.' +
				context.fmtIdentifier(getName()) + ' ' +
				context.fmtIdentifier(getDesc());
	}
}
