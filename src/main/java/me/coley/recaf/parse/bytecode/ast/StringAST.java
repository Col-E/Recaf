package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.util.EscapeUtil;

/**
 * String AST.
 *
 * @author Matt
 */
public class StringAST extends AST {
	private final String value;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param value
	 * 		Value.
	 */
	public StringAST(int line, int start, String value) {
		super(line, start);
		this.value = value;
	}

	/**
	 * @return Value.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @return Value without escapes.
	 */
	public String getUnescapedValue() {
		return EscapeUtil.unescape(value);
	}

	@Override
	public String print() {
		return "\"" + value + "\"";
	}
}
