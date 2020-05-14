package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.Variables;
import me.coley.recaf.util.EscapeUtil;
import me.coley.recaf.util.StringUtil;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.Type;

import java.util.Map;

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
		return getOpcode().print() + " " + StringUtil.escape(content.print());
	}

	@Override
	public AbstractInsnNode compile(Map<String, LabelNode> labels, Variables variables) {
		Object value = null;
		if(content instanceof StringAST)
			value = EscapeUtil.unescape(((StringAST) content).getValue());
		else if(content instanceof NumberAST)
			value = ((NumberAST) content).getValue();
		else if(content instanceof DescAST)
			value = Type.getType(((DescAST) content).getDesc());
		else if(content instanceof HandleAST)
			value = ((HandleAST) content).compile();
		return new LdcInsnNode(value);
	}
}
