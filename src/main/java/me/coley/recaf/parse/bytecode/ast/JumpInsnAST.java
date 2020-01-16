package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.Variables;
import org.objectweb.asm.tree.*;

import java.util.Map;

/**
 * Jump instruction AST.
 *
 * @author Matt
 */
public class JumpInsnAST extends InsnAST {
	private final NameAST label;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param opcode
	 * 		Opcode AST.
	 * @param label
	 * 		Label name AST.
	 */
	public JumpInsnAST(int line, int start, OpcodeAST opcode, NameAST label) {
		super(line, start, opcode);
		this.label = label;
		addChild(label);
	}

	/**
	 * @return Label name AST.
	 */
	public NameAST getLabel() {
		return label;
	}

	@Override
	public String print() {
		return getOpcode().print() + " " + label.print();
	}

	@Override
	public AbstractInsnNode compile(Map<String, LabelNode> labels, Variables variables) {
		return new JumpInsnNode(getOpcode().getOpcode(), labels.get(getLabel().getName()));
	}
}
