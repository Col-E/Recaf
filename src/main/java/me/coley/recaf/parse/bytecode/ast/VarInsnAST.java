package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import me.coley.recaf.parse.bytecode.Variables;
import org.objectweb.asm.tree.*;

import java.util.Map;

/**
 * Variable instruction AST.
 *
 * @author Matt
 */
public class VarInsnAST extends InsnAST implements VariableReference {
	private final NameAST variable;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param opcode
	 * 		Opcode AST.
	 * @param variable
	 * 		Variable name AST.
	 */
	public VarInsnAST(int line, int start, OpcodeAST opcode, NameAST variable) {
		super(line, start, opcode);
		this.variable = variable;
		addChild(variable);
	}

	@Override
	public NameAST getVariableName() {
		return variable;
	}

	@Override
	public String print() {
		return getOpcode().print() + " " + variable.print();
	}

	@Override
	public AbstractInsnNode compile(Map<String, LabelNode> labels, Variables variables) throws AssemblerException {
		return new VarInsnNode(getOpcode().getOpcode(), getVariableIndex(variables));
	}
}
