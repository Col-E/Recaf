package me.coley.recaf.assemble.ast;

/**
 * An element that refers to a single variable via an identifier.
 *
 * @author Matt Coley
 */
public interface VariableReference {
	/**
	 * @return Variable identifier.
	 */
	String getVariableIdentifier();
	/**
	 * @return Variable type descriptor.
	 */
	String getVariableDescriptor();
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
