package me.coley.recaf.parse.bytecode.ast;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;

/**
 * Method declaration AST.
 *
 * @author Matt
 */
public class DefinitionAST extends AST {
	private final List<DefinitionModifierAST> modifiers = new ArrayList<>();
	private final List<DefinitionArgAST> arguments = new ArrayList<>();
	private final NameAST name;
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
	public DefinitionAST(int line, int start, NameAST name, DescAST retType) {
		super(line, start);
		this.name = name;
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
	 * @return Method name.
	 */
	public NameAST getName() {
		return name;
	}

	/**
	 * @return Method return type.
	 */
	public DescAST getReturnType() {
		return retType;
	}

	@Override
	public String print() {
		String modifiersStr = getModifiers().stream().map(AST::print).collect(joining(" "));
		String argumentsStr = getArguments().stream().map(AST::print).collect(joining(", "));
		String ret = getReturnType().print();
		return "DEFINE " + modifiersStr + " " + getName().print() + "(" + argumentsStr + ")" + ret;
	}
}
