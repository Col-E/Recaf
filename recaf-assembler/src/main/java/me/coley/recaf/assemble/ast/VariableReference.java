package me.coley.recaf.assemble.ast;

import me.coley.recaf.util.EscapeUtil;

/**
 * An element that refers to a single variable via an identifier.
 *
 * @author Matt Coley
 */
public interface VariableReference extends Element {
	/**
	 * @return Variable identifier.
	 */
	String getVariableIdentifier();

	/**
	 * @return Whitespace escaped variable identifier.
	 */
	default String getEscapedVariableIdentifier() {
		return EscapeUtil.formatIdentifier(getVariableIdentifier());
	}

	/**
	 * @return Variable type descriptor.
	 */
	String getVariableDescriptor();

	/**
	 * @return Whitespace escaped variable descriptor.
	 */
	default String getEscapedVariableDescriptor() {
		return EscapeUtil.formatIdentifier(getVariableDescriptor());
	}

	/**
	 * Always {@code false} for instructions, but things like {@link me.coley.recaf.assemble.ast.arch.MethodParameter}
	 * will be {@code true} since they can explicitly declare the object's type.
	 *
	 * @return {@code true} when the {@link #getVariableDescriptor()} denoting an object type knows the exact type.
	 * {@code false} when the type is not known, therefore it is assumed to be {@link me.coley.recaf.util.Types#OBJECT_TYPE}.
	 */
	boolean isObjectDescriptorExplicitlyDeclared();

	/**
	 * @return Variable operation.
	 */
	OpType getVariableOperation();

	/**
	 * Type of action being done with variable.
	 */
	enum OpType {
		LOAD,
		ASSIGN,
		UPDATE
	}
}
