package me.coley.recaf.parse.bytecode.ast;

import org.objectweb.asm.Type;

/**
 * Field default value AST.
 *
 * @author Matt
 */
public class DefaultValueAST extends AST {
	private final AST content;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param content
	 * 		Constant value AST.
	 */
	public DefaultValueAST(int line, int start, AST content) {
		super(line, start);
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
		return "VALUE " + content.print();
	}

	/**
	 * @return Field value.
	 */
	public Object toValue() {
		Object value = null;
		if(content instanceof StringAST)
			value = ((StringAST) content).getUnescapedValue();
		else if(content instanceof NumberAST)
			value = ((NumberAST) content).getValue();
		else if(content instanceof DescAST)
			value = Type.getType(((DescAST) content).getDesc());
		else if(content instanceof HandleAST)
			value = ((HandleAST) content).compile();
		return value;
	}
}
