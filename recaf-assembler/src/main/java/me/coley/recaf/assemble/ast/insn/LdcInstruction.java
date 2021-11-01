package me.coley.recaf.assemble.ast.insn;

import me.coley.recaf.assemble.ast.ArgType;
import org.objectweb.asm.Type;

/**
 * LDC instruction.
 *
 * @author Matt Coley
 */
public class LdcInstruction extends AbstractInstruction {
	private final ArgType type;
	private final Object value;

	/**
	 * @param opcode
	 * 		LDC instruction opcode.
	 * @param value
	 * 		String value.
	 */
	public LdcInstruction(String opcode, String value) {
		this(opcode, value, ArgType.STRING);
	}

	/**
	 * @param opcode
	 * 		LDC instruction opcode.
	 * @param value
	 * 		Int value.
	 */
	public LdcInstruction(String opcode, int value) {
		this(opcode, value, ArgType.INTEGER);
	}

	/**
	 * @param opcode
	 * 		LDC instruction opcode.
	 * @param value
	 * 		Type value.
	 */
	public LdcInstruction(String opcode, Type value) {
		this(opcode, value, ArgType.TYPE);
	}

	/**
	 * @param opcode
	 * 		LDC instruction opcode.
	 * @param value
	 * 		Float value.
	 */
	public LdcInstruction(String opcode, float value) {
		this(opcode, value, ArgType.FLOAT);
	}

	/**
	 * @param opcode
	 * 		LDC instruction opcode.
	 * @param value
	 * 		Double value.
	 */
	public LdcInstruction(String opcode, double value) {
		this(opcode, value, ArgType.DOUBLE);
	}

	/**
	 * @param opcode
	 * 		LDC instruction opcode.
	 * @param value
	 * 		Long value.
	 */
	public LdcInstruction(String opcode, long value) {
		this(opcode, value, ArgType.LONG);
	}

	private LdcInstruction(String opcode, Object value, ArgType type) {
		super(opcode);
		this.value = value;
		this.type = type;
	}

	/**
	 * <b>Note</b>: Strings are not unescaped. They appear as the literal text that was present at parse-time.
	 *
	 * @return Constant value.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * @return Type of content of the {@link #getValue() constant value}.
	 */
	public ArgType getValueType() {
		return type;
	}

	@Override
	public String print() {
		switch (getValueType()) {
			case STRING:
				// TODO: Handle string escapes
				//  - escape util is in core, so we can't access it here...
				//  - one fix would be to move it to this module and have core require this module
				return String.format("%s \"%s\"", getOpcode(), getValue());
			case TYPE:
				Type type = (Type) getValue();
				if (type.getSort() == Type.OBJECT)
					return String.format("%s %s", getOpcode(), type.getInternalName());
				else
					return String.format("%s %s", getOpcode(), type);
			case INTEGER:
				return String.format("%s %d", getOpcode(), getValue());
			case LONG:
				return String.format("%s %dL", getOpcode(), getValue());
			case FLOAT:
				return String.format("%s %fF", getOpcode(), getValue());
			case DOUBLE:
				return String.format("%s %fD", getOpcode(), getValue());
			default:
				throw new IllegalStateException("Unhandled constant value type: " + getValueType());
		}
	}

}
