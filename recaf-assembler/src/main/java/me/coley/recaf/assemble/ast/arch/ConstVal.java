package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.ArgType;
import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.CodeEntry;
import me.coley.recaf.assemble.util.Placeholder;

/**
 * Details a constant-value assigned to a {@link FieldDefinition}.
 * Do note that these values may not be used if you just slap it into any old field definition, see:
 * <a href="https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.7.2">
 * 4.7.2. The ConstantValue Attribute</a>
 *
 * @author Matt Coley
 */
public class ConstVal extends BaseElement implements CodeEntry {
	private final ArgType type;
	private final Object value;

	/**
	 * @param value
	 * 		String value.
	 */
	public ConstVal(String value) {
		this(value, ArgType.STRING);
	}

	/**
	 * @param value
	 * 		Int value.
	 */
	public ConstVal(int value) {
		this(value, ArgType.INTEGER);
	}

	/**
	 * @param value
	 * 		Float value.
	 */
	public ConstVal(float value) {
		this(value, ArgType.FLOAT);
	}

	/**
	 * @param value
	 * 		Double value.
	 */
	public ConstVal(double value) {
		this(value, ArgType.DOUBLE);
	}

	/**
	 * @param value
	 * 		Long value.
	 */
	public ConstVal(long value) {
		this(value, ArgType.LONG);
	}

	private ConstVal(Object value, ArgType type) {
		this.type = type;
		this.value = value;
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
	public void insertInto(Code code) {
		code.setConstVal(this);
	}

	@Override
	public String print() {
		String op = "CONST-VALUE";
		switch (getValueType()) {
			case STRING:
				// We escape whatever string value is here because it makes parsing much simpler.
				// However, if the user wishes to insert unescaped text that's on them.
				return String.format("%s \"%s\"", op, Placeholder.escape((String) getValue()));
			case INTEGER:
				return String.format("%s %d", op, getValue());
			case LONG:
				return String.format("%s %dL", op, getValue());
			case FLOAT:
				return String.format("%s %fF", op, getValue());
			case DOUBLE:
				return String.format("%s %fD", op, getValue());
			case TYPE:
				throw new IllegalStateException("Constant values do not allow type values!");
			default:
				throw new IllegalStateException("Unhandled constant value type: " + getValueType());
		}
	}
}
