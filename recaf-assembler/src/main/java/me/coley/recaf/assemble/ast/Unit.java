package me.coley.recaf.assemble.ast;

import me.coley.recaf.assemble.ast.arch.AbstractDefinition;
import me.coley.recaf.assemble.ast.arch.ClassDefinition;
import me.coley.recaf.assemble.ast.arch.FieldDefinition;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;

/**
 * The complete unit of a field or method.
 *
 * @author Matt Coley
 */
public class Unit extends BaseElement {
	private final AbstractDefinition definition;

	/**
	 * @param definition
	 * 		Field or method definition.
	 */
	public Unit(AbstractDefinition definition) {
		this.definition = child(definition);
	}

	@Override
	public String print(PrintContext context) {
		return definition.print(context);
	}

	/**
	 * @return {@code true} if the definition is a class.
	 */
	public boolean isClass() {
		return definition.isClass();
	}

	/**
	 * @return {@code true} if the definition is a {@link #isField() field} or {@link #isMethod() method}.
	 */
	public boolean isMember() {
		return isField() || isMethod();
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
	 * @return Class, field, or method definition.
	 */
	public AbstractDefinition getDefinition() {
		return definition;
	}

	/**
	 * @return Class definition.
	 *
	 * @throws IllegalStateException
	 * 		If the definition is not a class
	 * @see #isClass()
	 */
	public ClassDefinition getDefinitionAsClass() {
		if (!isClass())
			throw new IllegalStateException("Not a class");
		return (ClassDefinition) definition;
	}

	/**
	 * @return Field definition.
	 *
	 * @throws IllegalStateException
	 * 		If the definition is not a class
	 * @see #isMember()
	 * @see #isField()
	 */
	public FieldDefinition getDefinitionAsField() {
		if (!isField())
			throw new IllegalStateException("Not a field");
		return (FieldDefinition) definition;
	}

	/**
	 * @return Method definition.
	 *
	 * @throws IllegalStateException
	 * 		If the definition is not a class
	 * @see #isMember()
	 * @see #isMethod()
	 */
	public MethodDefinition getDefinitionAsMethod() {
		if (!isMethod())
			throw new IllegalStateException("Not a method");
		return (MethodDefinition) definition;
	}
}
