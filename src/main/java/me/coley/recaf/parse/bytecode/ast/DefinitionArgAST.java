package me.coley.recaf.parse.bytecode.ast;

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
