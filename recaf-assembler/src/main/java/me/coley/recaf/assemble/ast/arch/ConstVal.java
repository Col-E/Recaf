package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.ArgType;
import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.util.EscapeUtil;

/**
 * Details a constant-value assigned to a {@link FieldDefinition}.
 * Do note that these values may not be used if the field is not static, see:
 * <a href="https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.7.2">
 * 4.7.2. The ConstantValue Attribute</a>
 *
 * @author Matt Coley
 */
public class ConstVal extends BaseElement {
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

	public ConstVal(Object value, ArgType type) {
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
	public String print(PrintContext context) {
		switch (getValueType()) {
			case STRING:
				// We escape whatever string value is here because it makes parsing much simpler.
				// However, if the user wishes to insert unescaped text that's on them.
				return "\"" + EscapeUtil.escape((String) getValue()) + "\"";
			case INTEGER:
				return getValue().toString();
			case LONG:
				return getValue().toString() + "L";
			case FLOAT:
				return getValue().toString() + "F";
			case DOUBLE:
				return getValue().toString() + "D";
			case TYPE:
				throw new IllegalStateException("Constant values do not allow type values!");
			default:
				throw new IllegalStateException("Unhandled constant value type: " + getValueType());
		}
	}
}
