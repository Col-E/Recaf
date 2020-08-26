package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.MethodCompilation;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import org.objectweb.asm.tree.*;

import java.util.Collections;
import java.util.List;

/**
 * Jump instruction AST.
 *
 * @author Matt
 */
public class JumpInsnAST extends InsnAST implements FlowController {
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
	public void compile(MethodCompilation compilation) throws AssemblerException {
		LabelNode label = compilation.getLabel(getLabel().getName());
		if (label == null)
			throw new AssemblerException("Specified destination label '" + getLabel().getName() +
					"' does not exist", getLine());
		compilation.addInstruction(new JumpInsnNode(getOpcode().getOpcode(), label), this);
	}

	@Override
	public List<String> targets() {
		return Collections.singletonList(getLabel().getName());
	}
}
