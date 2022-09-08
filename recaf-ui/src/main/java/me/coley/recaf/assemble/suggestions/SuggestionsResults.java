package me.coley.recaf.assemble.suggestions;

import java.util.stream.Stream;

/**
 * Suggestion result.
 *
 * @author xDark
 * @see Suggestion
 */
public final class SuggestionsResults {
	private final String input;
	private final Stream<Suggestion> result;

	/**
	 * @param input
	 * 		Auto-completion input.
	 * @param result
	 * 		Stream of suggestions.
	 */
	public SuggestionsResults(String input, Stream<Suggestion> result) {
		this.input = input;
		this.result = result;
	}

	/**
	 * @return Auto-completion input.
	 */
	public String getInput() {
		return input;
	}

	/**
	 * @return Stream of suggestions.
	 */
	public Stream<Suggestion> getValues() {
		return result;
	}
}
