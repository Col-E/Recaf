package me.coley.recaf.parse.bytecode.ast;

import org.objectweb.asm.Type;

/**
 * Method argument AST.
 *
 * @author Matt
 */
public class DefinitionArgAST extends AST implements VariableReference {
	private final DescAST desc;
	private final NameAST name;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param desc
	 * 		Argument type, in descriptor format.
	 * @param name
	 * 		Argument name.
	 */
	public DefinitionArgAST(int line, int start, DescAST desc, NameAST name) {
		super(line, start);
		this.desc = desc;
		this.name = name;
		addChild(desc);
		addChild(name);
	}

	@Override
	public NameAST getVariableName() {
		return name;
	}

	@Override
	public int getVariableSort() {
		return Type.getType(getDesc().getDesc()).getSort();
	}

	/**
	 * @return Argument type, in descriptor format.
	 */
	public DescAST getDesc() {
		return desc;
	}

	@Override
	public String print() {
		return getDesc().print() + " " + getVariableName().print();
	}
}
