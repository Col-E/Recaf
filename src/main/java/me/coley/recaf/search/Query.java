package me.coley.recaf.search;

import java.util.*;

/**
 * Query base.
 *
 * @author Matt
 */
public abstract class Query {
	private final QueryType type;
	protected final StringMatchMode stringMode;
	protected final List<SearchResult> matched = new ArrayList<>();

	/**
	 * Baseline query.
	 *
	 * @param type
	 * 		Type of query implementation.
	 * @param stringMode
	 * 		How to match strings.
	 */
	public Query(QueryType type, StringMatchMode stringMode) {
		this.type = type;
		this.stringMode = stringMode;
	}

	/**
	 * @return Implementation type.
	 */
	public QueryType getType() {
		return type;
	}

	/**
	 * @param type
	 * 		Implementation type.
	 *
	 * @return {@code true} if the query's type matches the given type.
	 */
	public boolean isType(QueryType type) {
		return this.type.equals(type);
	}

	/**
	 * A temporary storage of results.
	 *
	 * @return List of results matched.
	 */
	public List<SearchResult> getMatched() {
		return matched;
	}
}
