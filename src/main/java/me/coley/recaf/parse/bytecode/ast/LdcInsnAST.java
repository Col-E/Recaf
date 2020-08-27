package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.MethodCompilation;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.Type;

/**
 * Load constant instruction AST.
 *
 * @author Matt
 */
public class LdcInsnAST extends InsnAST {
	private final AST content;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param opcode
	 * 		Opcode AST.
	 * @param content
	 * 		Constant value AST.
	 */
	public LdcInsnAST(int line, int start, OpcodeAST opcode, AST content) {
		super(line, start, opcode);
		this.content = content;
		addChild(content);
	}

	/**
	 * @return Constant value AST.
	 */
	public AST getContent() {
		return content;
	}

	@Override
	public String print() {
		return getOpcode().print() + " " + content.print();
	}

	@Override
	public void compile(MethodCompilation compilation) throws AssemblerException {
		Object value = null;
		if(content instanceof StringAST)
			value = ((StringAST) content).getUnescapedValue();
		else if(content instanceof NumberAST)
			value = ((NumberAST) content).getValue();
		else if(content instanceof DescAST)
			value = Type.getType(((DescAST) content).getDesc());
		else if(content instanceof HandleAST)
			value = ((HandleAST) content).compile();
		compilation.addInstruction(new LdcInsnNode(value), this);
	}
}
