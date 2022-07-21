package me.coley.recaf.assemble.suggestions;

import java.util.stream.Stream;

/**
 * Suggestion result.
 *
 * @author xDark
 */
public final class SuggestionResult {
	private final String input;
	private final Stream<String> result;

	/**
	 * @param input
	 * 		Auto-completion input.
	 * @param result
	 * 		Stream of suggestions.
	 */
	public SuggestionResult(String input, Stream<String> result) {
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
	public Stream<String> getResult() {
		return result;
	}
}
