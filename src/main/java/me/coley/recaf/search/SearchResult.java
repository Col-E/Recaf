package me.coley.recaf.search;

/**
 * Search result base.
 *
 * @author Matt
 */
public abstract class SearchResult implements Comparable<SearchResult> {
	private Context<?> context;

	/**
	 * Sets the context <i>(Where the result is located)</i> of the search result.
	 *
	 * @param context
	 * 		Context of the result.
	 */
	public void setContext(Context<?> context) {
		this.context = context;
	}

	/**
	 * @return Context of the result.
	 */
	public Context<?> getContext() {
		return context;
	}

	@Override
	public int compareTo(SearchResult other) {
		return context.compareTo(other.context);
	}

	/**
	 * @param other
	 * 		Result to be compared
	 *
	 * @return {@code true} if both contexts are considered similar.
	 *
	 * @see Context#isSimilar(Context)
	 */
	public boolean isContextSimilar(SearchResult other) {
		return context != null && other.context != null && context.isSimilar(other.context);
	}

	@Override
	public String toString() {
		throw new UnsupportedOperationException("Unimplemented result string representation");
	}
}
