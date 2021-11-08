package me.coley.recaf.assemble.validation;

/**
 * Base visitor model for {@link Validator}s.
 *
 * @author Matt Coley
 * @param <V> Validator type the visitor is related to.
 */
public interface ValidationVisitor<V extends Validator> {
	/**
	 * @param validator Validator being visited.
	 */
	void visit(V validator);
}
