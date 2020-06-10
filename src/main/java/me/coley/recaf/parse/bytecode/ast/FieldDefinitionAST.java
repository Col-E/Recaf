package me.coley.recaf.parse.bytecode.ast;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;

/**
 * Field declaration AST.
 *
 * @author Matt
 */
public class FieldDefinitionAST extends AST {
	private final List<DefinitionModifierAST> modifiers = new ArrayList<>();
	private final NameAST name;
	private final DescAST type;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param name
	 * 		Field name.
	 * @param type
	 * 		Field return type.
	 */
	public FieldDefinitionAST(int line, int start, NameAST name, DescAST type) {
		super(line, start);
		this.name = name;
		this.type = type;
	}

	/**
	 * @return Field access modifier nodes.
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
	 * @return Field name.
	 */
	public NameAST getName() {
		return name;
	}

	/**
	 * @return Field type.
	 */
	public DescAST getType() {
		return type;
	}

	@Override
	public String print() {
		String modifiersStr = getModifiers().stream().map(AST::print).collect(joining(" "));
		return "FIELD " + modifiersStr + " " + getType().print() + " " + getName().print();
	}
}
