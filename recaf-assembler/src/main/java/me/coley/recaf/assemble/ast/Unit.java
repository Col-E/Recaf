package me.coley.recaf.assemble.ast;

import me.coley.recaf.assemble.ast.arch.MemberDefinition;

/**
 * The complete unit of a field or method.
 *
 * @author Matt Coley
 */
public class Unit extends BaseElement {
	private final MemberDefinition definition;
	private final Code code;

	/**
	 * @param definition
	 * 		Field or method definition.
	 * @param code
	 * 		Optional code value; typically {@code null} if the definition represents a field or an abstract method.
	 */
	public Unit(MemberDefinition definition, Code code) {
		this.definition = definition;
		this.code = code;
	}

	@Override
	public String print() {
		StringBuilder sb = new StringBuilder(definition.print());
		if (code != null) {
			sb.append('\n').append(code.print());
		}
		return sb.toString();
	}

	/**
	 * @return {@code true} if the definition is a field.
	 */
	public boolean isMethod() {
		return definition.isMethod();
	}

	/**
	 * @return {@code true} if the definition is a method.
	 */
	public boolean isField() {
		return definition.isField();
	}

	/**
	 * @return Field or method definition.
	 */
	public MemberDefinition getDefinition() {
		return definition;
	}

	/**
	 * @return Optional code value; typically {@code null} if the definition represents a field or an abstract method.
	 */
	public Code getCode() {
		return code;
	}
}
