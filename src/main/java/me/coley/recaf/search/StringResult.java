package me.coley.recaf.search;

/**
 * Search result of a String.
 *
 * @author Matt
 */
public class StringResult extends SearchResult {
	private final String text;

	/**
	 * Constructs a class result.
	 *
	 * @param text
	 * 		Matched text.
	 */
	public StringResult(String text) {
		this.text = text;
	}

	/**
	 * @return Matched text.
	 */
	public String getText() {
		return text;
	}
}
