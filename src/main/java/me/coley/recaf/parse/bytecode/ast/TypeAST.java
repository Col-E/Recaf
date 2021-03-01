package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.util.EscapeUtil;

/**
 * Internal name AST.
 *
 * @author Matt
 */
public class TypeAST extends AST {
	private final String type;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param type
	 * 		Internal type name.
	 */
	public TypeAST(int line, int start, String type) {
		super(line, start);
		this.type = type;
	}

	/**
	 * @return Internal type.
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return Internal type without escapes.
	 */
	public String getUnescapedType() {
		return EscapeUtil.unescape(type);
	}

	@Override
	public String print() {
		return type;
	}
}
