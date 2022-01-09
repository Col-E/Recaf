package me.coley.recaf.assemble.validation;

/**
 * Base visitor model for {@link Validator}s.
 *
 * @param <V>
 * 		Validator type the visitor is related to.
 * @param <E>
 * 		Validation exception type.
 *
 * @author Matt Coley
 */
public interface ValidationVisitor<V extends Validator<E>, E extends Exception> {
	/**
	 * @param validator
	 * 		Validator being visited.
	 *
	 * @throws E
	 * 		Thrown when visiting failed.
	 */
	void visit(V validator) throws E;
}
