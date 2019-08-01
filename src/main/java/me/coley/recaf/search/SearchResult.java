package me.coley.recaf.search;

/**
 * Search result base.
 *
 * @author Matt
 */
public abstract class SearchResult {
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
}
