package me.coley.recaf.parse.bytecode.ast;

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

	@Override
	public String print() {
		return type;
	}
}
