package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.Variables;
import org.objectweb.asm.tree.*;

import java.util.Map;

/**
 * MultiANewArray instruction AST.
 *
 * @author Matt
 */
public class MultiArrayInsnAST extends InsnAST {
	private final TypeAST type;
	private final NumberAST dims;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param opcode
	 * 		Opcode AST.
	 * @param type
	 * 		Type AST.
	 * @param dims
	 * 		Dimension number AST.
	 */
	public MultiArrayInsnAST(int line, int start, OpcodeAST opcode, TypeAST type, NumberAST dims) {
		super(line, start, opcode);
		this.type = type;
		this.dims = dims;
		addChild(type);
		addChild(dims);
	}

	/**
	 * @return Type AST.
	 */
	public TypeAST getType() {
		return type;
	}

	/**
	 * @return Dimension number AST.
	 */
	public NumberAST getDimensions() {
		return dims;
	}

	@Override
	public String print() {
		return getOpcode().print() + " " + type.print() + " " + dims.print();
	}

	@Override
	public AbstractInsnNode compile(Map<String, LabelNode> labels, Variables variables) {
		return new MultiANewArrayInsnNode(getType().getType(), getDimensions().getIntValue());
	}
}
