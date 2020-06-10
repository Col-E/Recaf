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

	@Override
	public int compareTo(SearchResult other) {
		int ret = super.compareTo(other);
		if(ret == 0) {
			if(other instanceof StringResult) {
				StringResult otherResult = (StringResult) other;
				return text.compareTo(otherResult.text);
			}
		}
		return ret;
	}

	@Override
	public String toString() {
		return text.replace("\n", "\\n").replace("\t", "\\t");
	}
}
