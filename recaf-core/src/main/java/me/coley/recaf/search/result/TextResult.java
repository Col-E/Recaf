package me.coley.recaf.search.result;

/**
 * Result containing some matched text.
 *
 * @author Matt Coley
 */
public class TextResult extends Result {
	private final String matchedText;

	/**
	 * @param builder
	 * 		Builder containing information about the result.
	 * @param matchedText
	 * 		The text matched.
	 */
	public TextResult(ResultBuilder builder, String matchedText) {
		super(builder);
		this.matchedText = matchedText;
	}

	/**
	 * @return The text matched.
	 */
	public String getMatchedText() {
		return matchedText;
	}

	@Override
	protected Object getValue() {
		return getMatchedText();
	}
}
