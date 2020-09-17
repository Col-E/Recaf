package me.coley.recaf.parse.bytecode.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * Method declaration AST.
 *
 * @author Matt
 */
public class MethodDefinitionAST extends DefinitionAST {
	private final List<DefinitionArgAST> arguments = new ArrayList<>();
	private final DescAST retType;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param name
	 * 		Method name.
	 * @param retType
	 * 		Method return type.
	 */
	public MethodDefinitionAST(int line, int start, NameAST name, DescAST retType) {
		super(line, start, name);
		this.retType = retType;
	}

	/**
	 * @return Method access modifier nodes.
	 */
	public List<DefinitionModifierAST> getModifiers() {
		return modifiers;
	}

	/**
	 * @param modifier
	 * 		Modifier node to add.
	 */
	public void addModifier(DefinitionModifierAST modifier) {
		modifiers.add(modifier);
		addChild(modifier);
	}

	/**
	 * @return Combined modifiers.
	 */
	public int getModifierMask() {
		return search(DefinitionModifierAST.class).stream()
				.mapToInt(DefinitionModifierAST::getValue)
				.reduce(0, (a, b) -> a | b);
	}

	/**
	 * @return Method parameter nodes.
	 */
	public List<DefinitionArgAST> getArguments() {
		return arguments;
	}

	/**
	 * @param arg
	 * 		Argument node to add.
	 */
	public void addArgument(DefinitionArgAST arg) {
		arguments.add(arg);
		addChild(arg);
	}

	/**
	 * @return Method return type.
	 */
	public DescAST getReturnType() {
		return retType;
	}


	/**
	 * @return Combined method descriptor of argument children and return type child.
	 */
	@Override
	public String getDescriptor() {
		String args = search(DefinitionArgAST.class).stream()
				.map(ast -> ast.getDesc().getDesc())
				.collect(Collectors.joining());
		String end = getReturnType().getDesc();
		return "(" + args + ")" + end;
	}

	@Override
	public String print() {
		String modifiersStr = getModifiers().stream().map(AST::print).collect(joining(" "));
		String argumentsStr = getArguments().stream().map(AST::print).collect(joining(", "));
		String ret = getReturnType().print();
		return "DEFINE " + modifiersStr + " " + getName().print() + "(" + argumentsStr + ")" + ret;
	}
}
