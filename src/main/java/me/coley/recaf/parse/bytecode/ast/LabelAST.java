package me.coley.recaf.parse.bytecode.ast;

/**
 * Label AST.
 *
 * @author Matt
 */
public class LabelAST extends AST {
	private final NameAST name;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param name
	 * 		Label name.
	 */
	public LabelAST(int line, int start, NameAST name) {
		super(line, start);
		this.name = name;
	}

	/**
	 * @return Name AST.
	 */
	public NameAST getName() {
		return name;
	}


	@Override
	public String print() {
		return name.print() + ":";
	}
}
