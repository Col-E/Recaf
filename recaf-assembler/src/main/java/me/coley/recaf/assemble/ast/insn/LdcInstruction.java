package me.coley.recaf.assemble.ast.insn;

import me.coley.recaf.assemble.ast.ArgType;
import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.util.EscapeUtil;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
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
	public LdcInstruction(int opcode, String value) {
		this(opcode, value, ArgType.STRING);
	}

	/**
	 * @param opcode
	 * 		LDC instruction opcode.
	 * @param value
	 * 		Type value.
	 */
	public LdcInstruction(int opcode, Type value) {
		this(opcode, value, ArgType.TYPE);
	}

	/**
	 * @param opcode
	 * 		LDC instruction opcode.
	 * @param value
	 * 		Handle value.
	 */
	public LdcInstruction(int opcode, HandleInfo value) {
		this(opcode, value, ArgType.HANDLE);
	}

	/**
	 * @param opcode
	 * 		LDC instruction opcode.
	 * @param value
	 * 		Handle value.
	 */
	public LdcInstruction(int opcode, Handle value) {
		this(opcode, value, ArgType.HANDLE);
	}

	/**
	 * @param opcode
	 * 		LDC instruction opcode.
	 * @param value
	 * 		Int value.
	 */
	public LdcInstruction(int opcode, int value) {
		this(opcode, value, ArgType.INTEGER);
	}

	/**
	 * @param opcode
	 * 		LDC instruction opcode.
	 * @param value
	 * 		Float value.
	 */
	public LdcInstruction(int opcode, float value) {
		this(opcode, value, ArgType.FLOAT);
	}

	/**
	 * @param opcode
	 * 		LDC instruction opcode.
	 * @param value
	 * 		Double value.
	 */
	public LdcInstruction(int opcode, double value) {
		this(opcode, value, ArgType.DOUBLE);
	}

	/**
	 * @param opcode
	 * 		LDC instruction opcode.
	 * @param value
	 * 		Long value.
	 */
	public LdcInstruction(int opcode, long value) {
		this(opcode, value, ArgType.LONG);
	}

	public LdcInstruction(int opcode, Object value, ArgType type) {
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
			return new LdcInstruction(Opcodes.LDC, (String) value);
		else if (value instanceof Integer)
			return new LdcInstruction(Opcodes.LDC, (int) value);
		else if (value instanceof Float)
			return new LdcInstruction(Opcodes.LDC, (float) value);
		else if (value instanceof Double)
			return new LdcInstruction(Opcodes.LDC, (double) value);
		else if (value instanceof Long)
			return new LdcInstruction(Opcodes.LDC, (long) value);
		else if (value instanceof Type)
			return new LdcInstruction(Opcodes.LDC, (Type) value);
		else if (value instanceof Handle)
			return new LdcInstruction(Opcodes.LDC, (Handle) value);
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
				return getOpcode() + " \"" + EscapeUtil.escape((String) getValue()) + '\"';
			case TYPE:
				Type type = (Type) getValue();
				if (type.getSort() == Type.OBJECT)
					return getOpcode() + " .type " + type.getInternalName();
				else
					return getOpcode() + " .type " + type;
			case INTEGER:
			case DOUBLE:
				return getOpcode() + " " + getValue();
			case LONG:
				return getOpcode() + " " + getValue() + 'L';
			case FLOAT:
				return getOpcode() + " " + getValue() + 'f';
			case HANDLE:
				return getOpcode() + ' ' + ".handle " + ((HandleInfo) getValue()).print();
			default:
				throw new IllegalStateException("Unhandled constant value type: " + getValueType());
		}
	}
}
