package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.MethodCompilation;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;

/**
 * Label AST.
 *
 * @author Matt
 */
public class LabelAST extends AST implements Compilable {
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

	@Override
	public void compile(MethodCompilation compilation) throws AssemblerException {
		compilation.addInstruction(compilation.getLabel(getName().getName()), this);
	}
}
