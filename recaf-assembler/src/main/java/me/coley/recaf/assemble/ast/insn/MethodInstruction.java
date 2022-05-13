package me.coley.recaf.assemble.ast.insn;

/**
 * Method reference instruction.
 *
 * @author Matt Coley
 */
public class MethodInstruction extends AbstractInstruction {
	private final String owner;
	private final String name;
	private final String desc;

	/**
	 * @param opcode
	 * 		Method instruction.
	 * @param owner
	 * 		Class declaring the method.
	 * @param name
	 * 		Name of the method.
	 * @param desc
	 * 		Type descriptor of the method.
	 */
	public MethodInstruction(int opcode, String owner, String name, String desc) {
		super(opcode);
		this.owner = owner;
		this.name = name;
		this.desc = desc;
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

	@Override
	public InstructionType getInsnType() {
		return InstructionType.METHOD;
	}

	@Override
	public String print() {
		return String.format("%s %s.%s%s", getOpcode(), getOwner(), getName(), getDesc());
	}
}
