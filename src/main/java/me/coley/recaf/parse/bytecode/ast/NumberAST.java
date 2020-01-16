package me.coley.recaf.parse.bytecode.ast;

/**
 * Generic number AST.
 *
 * @author Matt
 */
public class NumberAST extends AST {
	private final Number value;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param value
	 * 		Numeric value.
	 */
	public NumberAST(int line, int start, Number value) {
		super(line, start);
		this.value = value;
	}

	/**
	 * @return value.
	 */
	public Number getValue() {
		return value;
	}

	/**
	 * @return value as int.
	 */
	public int getIntValue() {
		return value.intValue();
	}

	/**
	 * @return value as long.
	 */
	public long getLongValue() {
		return value.longValue();
	}

	/**
	 * @return value as float.
	 */
	public float getFloatValue() {
		return value.floatValue();
	}

	/**
	 * @return value as double.
	 */
	public double getDoubleValue() {
		return value.doubleValue();
	}

	@Override
	public String print() {
		String val = String.valueOf(value);
		String suffix = "";
		if (value instanceof Long)
			suffix = "L";
		else if (value instanceof Float)
			suffix = "F";
		return val + suffix;
	}
}
