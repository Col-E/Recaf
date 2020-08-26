package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.MethodCompilation;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import org.objectweb.asm.tree.*;

/**
 * Base instruction AST.
 *
 * @author Matt
 */
public class InsnAST extends AST implements Instruction {
	private final OpcodeAST opcode;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param opcode
	 * 		Opcode AST.
	 */
	public InsnAST(int line, int start, OpcodeAST opcode) {
		super(line, start);
		this.opcode = opcode;
		addChild(opcode);
	}

	@Override
	public OpcodeAST getOpcode() {
		return opcode;
	}

	@Override
	public String print() {
		return opcode.print();
	}

	@Override
	public void compile(MethodCompilation compilation) throws AssemblerException {
		compilation.addInstruction(new InsnNode(getOpcode().getOpcode()), this);
	}
}
