package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.MethodCompilation;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/**
 * Variable instruction AST.
 *
 * @author Matt
 */
public class IincInsnAST extends InsnAST implements VariableReference {
	private final NameAST variable;
	private final NumberAST incr;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param opcode
	 * 		Opcode AST.
	 * @param variable
	 * 		Variable name AST.
	 * @param incr
	 * 		Increment value AST.
	 */
	public IincInsnAST(int line, int start, OpcodeAST opcode, NameAST variable, NumberAST incr) {
		super(line, start, opcode);
		this.variable = variable;
		this.incr = incr;
		addChild(variable);
		addChild(incr);
	}

	@Override
	public NameAST getVariableName() {
		return variable;
	}

	@Override
	public int getVariableSort() {
		return Type.INT;
	}

	/**
	 * @return Increment value AST.
	 */
	public NumberAST getIncrement() {
		return incr;
	}

	@Override
	public String print() {
		return getOpcode().print() + " " + variable.print() + " " + incr.print();
	}

	@Override
	public void compile(MethodCompilation compilation) throws AssemblerException {
		compilation.addInstruction(new IincInsnNode(getVariableIndex(compilation.getVariableNameCache()),
				getIncrement().getIntValue()), this);
	}
}
