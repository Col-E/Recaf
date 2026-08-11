package software.coley.recaf.services.search.query.structure;

/**
 * Count comparison mode.
 *
 * @author Matt Coley
 * @see CountConstraint
 */
public enum Arity {
	/**
	 * Requires the consumed count to equal the target.
	 */
	EXACT,

	/**
	 * Requires the consumed count to be at least the target.
	 */
	AT_LEAST,

	/**
	 * Requires the consumed count to be at most the target.
	 */
	AT_MOST
}
