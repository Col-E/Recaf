package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.MethodCompilation;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import org.objectweb.asm.tree.*;

/**
 * Line number instruction AST.
 *
 * @author Matt
 */
public class LineInsnAST extends InsnAST {
	private final NameAST label;
	private final NumberAST lineNumber;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param opcode
	 * 		Opcode AST.
	 * @param label
	 * 		Label name AST.
	 * @param lineNumber
	 * 		Line number AST.
	 */
	public LineInsnAST(int line, int start, OpcodeAST opcode, NameAST label, NumberAST lineNumber) {
		super(line, start, opcode);
		this.label = label;
		this.lineNumber = lineNumber;
		addChild(label);
		addChild(lineNumber);
	}

	/**
	 * @return Label name AST.
	 */
	public NameAST getLabel() {
		return label;
	}

	/**
	 * @return Line number AST.
	 */
	public NumberAST getLineNumber() {
		return lineNumber;
	}

	@Override
	public String print() {
		return getOpcode().print() + " " + label.print() + " " + lineNumber.print();
	}

	@Override
	public void compile(MethodCompilation compilation) throws AssemblerException {
		compilation.addInstruction(new LineNumberNode(getLineNumber().getIntValue(),
				compilation.getLabel(getLabel().getName())), this);
	}
}
