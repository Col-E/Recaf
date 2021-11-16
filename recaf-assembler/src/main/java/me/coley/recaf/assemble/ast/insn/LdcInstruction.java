package me.coley.recaf.assemble.ast.insn;

import me.coley.recaf.assemble.ast.ArgType;
import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.util.EscapeUtil;
import org.objectweb.asm.Handle;
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
	 * 		Type value.
	 */
	public LdcInstruction(String opcode, Type value) {
		this(opcode, value, ArgType.TYPE);
	}

	/**
	 * @param opcode
	 * 		LDC instruction opcode.
	 * @param value
	 * 		Handle value.
	 */
	public LdcInstruction(String opcode, Handle value) {
		this(opcode, value, ArgType.HANDLE);
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
	 * @param value
	 * 		Value of some unknown type.
	 *
	 * @return Ldc AST instance based on value.
	 */
	public static LdcInstruction of(Object value) {
		if (value instanceof String)
			return new LdcInstruction("LDC", (String) value);
		else if (value instanceof Integer)
			return new LdcInstruction("LDC", (int) value);
		else if (value instanceof Float)
			return new LdcInstruction("LDC", (float) value);
		else if (value instanceof Double)
			return new LdcInstruction("LDC", (double) value);
		else if (value instanceof Long)
			return new LdcInstruction("LDC", (long) value);
		else if (value instanceof Type)
			return new LdcInstruction("LDC", (Type) value);
		else if (value instanceof Handle)
			return new LdcInstruction("LDC", (Handle) value);
		else if (value == null)
			throw new IllegalStateException("LDC content must not be null!");
		throw new IllegalStateException("Unsupported LDC content type: " + value.getClass().getName());
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
	public InstructionType getInsnType() {
		return InstructionType.LDC;
	}

	@Override
	public String print() {
		switch (getValueType()) {
			case STRING:
				// We escape whatever string value is here because it makes parsing much simpler.
				// However, if the user wishes to insert unescaped text that's on them.
				return String.format("%s \"%s\"", getOpcode(), EscapeUtil.escape((String) getValue()));
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
			case HANDLE:
				String htext = "handle[" + ((HandleInfo) getValue()).print() + "]";
				return String.format("%s %s", getOpcode(), htext);
			default:
				throw new IllegalStateException("Unhandled constant value type: " + getValueType());
		}
	}
}
