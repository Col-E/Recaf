package me.coley.recaf.assemble.ast;

import me.coley.recaf.assemble.ast.arch.*;

/**
 * The complete unit of a field or method.
 *
 * @author Matt Coley
 */
public class Unit extends BaseElement {
	private final AbstractMemberDefinition definition;

	/**
	 * @param definition
	 * 		Field or method definition.
	 *
	 */
	public Unit(AbstractMemberDefinition definition) {
		this.definition = child(definition);
	}

	@Override
	public String print() {
		return definition.print();
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

	public boolean isClass() {
		return definition.isClass();
	}

	/**
	 * @return Field or method definition.
	 */
	public AbstractMemberDefinition getDefinition() {
		return definition;
	}

	/**
	 * @return Method definition.
	 * @throws IllegalStateException if the definition is not a method.
	 * @see #isMethod()
	 * @see #isField()
	 */
	public MethodDefinition getMethod() {
		if(!isMethod())
			throw new IllegalStateException("Not a method");
		return (MethodDefinition) definition;
	}

	/**
	 * @return Class definition.
	 * @throws IllegalStateException if the definition is not a class
	 * @see #isMethod()
	 * @see #isField()
	 * @see #isClass()
	 */
	public ClassDefinition getClassDefinition() {
		if(!isClass())
			throw new IllegalStateException("Not a class");
		return (ClassDefinition) definition;
	}

	/**
	 * @return Field definition.
	 * @throws IllegalStateException if the definition is not a field.
	 * @see #isMethod()
	 * @see #isField()
	 */
	public FieldDefinition getField() {
		if(!isField())
			throw new IllegalStateException("Not a field");
		return (FieldDefinition) definition;
	}
}
