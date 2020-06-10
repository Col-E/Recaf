package me.coley.recaf.parse.bytecode.ast;

/**
 * Method throws AST.
 *
 * @author Matt
 */
public class ThrowsAST extends AST {
	private final TypeAST type;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param type
	 * 		Exception type.
	 */
	public ThrowsAST(int line, int start, TypeAST type) {
		super(line, start);
		this.type = type;
		addChild(type);
	}

	/**
	 * @return Exception type.
	 */
	public TypeAST getType() {
		return type;
	}

	@Override
	public String print() {
		return "THROWS " + type.print();
	}
}
