package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.MethodCompilation;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import org.objectweb.asm.tree.*;

/**
 * MultiANewArray instruction AST.
 *
 * @author Matt
 */
public class MultiArrayInsnAST extends InsnAST {
	private final DescAST desc;
	private final NumberAST dims;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param opcode
	 * 		Opcode AST.
	 * @param desc
	 * 		Descriptor AST.
	 * @param dims
	 * 		Dimension number AST.
	 */
	public MultiArrayInsnAST(int line, int start, OpcodeAST opcode, DescAST desc, NumberAST dims) {
		super(line, start, opcode);
		this.desc = desc;
		this.dims = dims;
		addChild(desc);
		addChild(dims);
	}

	/**
	 * @return Descriptor AST.
	 */
	public DescAST getDesc() {
		return desc;
	}

	/**
	 * @return Dimension number AST.
	 */
	public NumberAST getDimensions() {
		return dims;
	}

	@Override
	public String print() {
		return getOpcode().print() + " " + desc.print() + " " + dims.print();
	}

	@Override
	public void compile(MethodCompilation compilation) throws AssemblerException {
		compilation.addInstruction(new MultiANewArrayInsnNode(getDesc().getDesc(),
				getDimensions().getIntValue()), this);
	}
}
