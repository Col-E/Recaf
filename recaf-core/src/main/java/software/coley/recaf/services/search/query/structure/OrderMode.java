package software.coley.recaf.services.search.query.structure;

/**
 * Candidate traversal mode for list matching.
 *
 * @author Matt Coley
 * @see ListMatcher
 */
public enum OrderMode {
	/**
	 * Matches candidates as an unordered collection.
	 */
	BAG,

	/**
	 * Matches candidates in their original order.
	 */
	SEQUENCE
}
