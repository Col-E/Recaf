package me.coley.recaf.search;

/**
 * Query to find strings matching the given pattern.
 *
 * @author Matt
 */
public class StringQuery extends Query {
	private final String pattern;

	/**
	 * Constructs a string matching query.
	 *
	 * @param pattern
	 * 		String pattern.
	 * @param stringMode
	 * 		How to match strings.
	 */
	public StringQuery(String pattern, StringMatchMode stringMode) {
		super(QueryType.CLASS_NAME, stringMode);
		this.pattern = pattern;
	}

	/**
	 * Adds a result if the given string matches the specified name pattern.
	 *
	 * @param text
	 * 		Text to match.
	 */
	public void match(String text) {
		if(stringMode.match(pattern, text)) {
			getMatched().add(new StringResult(text));
		}
	}
}
