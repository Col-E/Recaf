package me.coley.recaf.assemble.suggestions;

import me.coley.recaf.assemble.suggestions.type.Suggestion;

import java.util.stream.Stream;

/**
 * Suggestion result.
 *
 * @author xDark
 * @see Suggestion
 */
public final class SuggestionsResults {
	private final String input;
	private final Stream<? extends Suggestion> result;
	private boolean valid = true;

	/**
	 * @param input
	 * 		Auto-completion input.
	 * @param result
	 * 		Stream of suggestions.
	 */
	public SuggestionsResults(String input, Stream<? extends Suggestion> result) {
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
	public Stream<? extends Suggestion> getValues() {
		if(!valid) throw new IllegalStateException("Results are already invalidated!");
		return result;
	}

	public boolean isValid() {
		return valid;
	}

	public void invalidate() {
		valid = false;
	}
}
