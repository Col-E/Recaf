package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.MethodCompilation;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import org.objectweb.asm.tree.*;

/**
 * Type instruction AST.
 *
 * @author Matt
 */
public class TypeInsnAST extends InsnAST {
	private final TypeAST type;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param opcode
	 * 		Opcode AST.
	 * @param type
	 * 		Type AST.
	 */
	public TypeInsnAST(int line, int start, OpcodeAST opcode, TypeAST type) {
		super(line, start, opcode);
		this.type = type;
		addChild(type);
	}

	/**
	 * @return Type AST.
	 */
	public TypeAST getType() {
		return type;
	}

	@Override
	public String print() {
		return getOpcode().print() + " " + type.print();
	}

	@Override
	public void compile(MethodCompilation compilation) throws AssemblerException {
		compilation.addInstruction(new TypeInsnNode(getOpcode().getOpcode(), getType().getType()), this);
	}
}
