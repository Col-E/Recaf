package me.coley.recaf.parse.bytecode.ast;

/**
 * Generic name AST.
 *
 * @author Matt
 */
public class NameAST extends AST {
	private final String name;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param name
	 * 		Name.
	 */
	public NameAST(int line, int start, String name) {
		super(line, start);
		this.name = name;
	}

	/**
	 * @return Name.
	 */
	public String getName() {
		return name;
	}


	@Override
	public String print() {
		return name;
	}
}
