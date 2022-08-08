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
		return EscapeUtil.escapeSpace(getVariableIdentifier());
	}

	/**
	 * @return Variable type descriptor.
	 */
	String getVariableDescriptor();

	/**
	 * @return Whitespace escaped variable descriptor.
	 */
	default String getEscapedVariableDescriptor() {
		return EscapeUtil.escapeSpace(getVariableDescriptor());
	}

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
