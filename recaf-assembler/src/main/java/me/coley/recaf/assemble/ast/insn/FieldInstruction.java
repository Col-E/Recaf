package me.coley.recaf.assemble.ast.insn;

/**
 * Field reference instruction.
 *
 * @author Matt Coley
 */
public class FieldInstruction extends AbstractInstruction {
	private final String owner;
	private final String name;
	private final String desc;

	/**
	 * @param opcode
	 * 		Field instruction.
	 * @param owner
	 * 		Class declaring the field.
	 * @param name
	 * 		Name of the field.
	 * @param desc
	 * 		Type descriptor of the field.
	 */
	public FieldInstruction(String opcode, String owner, String name, String desc) {
		super(opcode);
		this.owner = owner;
		this.name = name;
		this.desc = desc;
	}

	/**
	 * @return Class declaring the field.
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @return Name of the field.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Type descriptor of the field.
	 */
	public String getDesc() {
		return desc;
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.FIELD;
	}

	@Override
	public String print() {
		return String.format("%s %s.%s %s", getOpcode(), getOwner(), getName(), getDesc());
	}
}
