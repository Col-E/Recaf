package me.coley.recaf.assemble.suggestions;

import me.xdrop.fuzzywuzzy.FuzzySearch;

import java.util.Comparator;

/**
 * Sorts {@link Suggestion} values based on a fuzzy match to the input search text.
 *
 * @author Matt Coley
 */
public class FuzzySuggestionComparator implements Comparator<Suggestion> {
	private final String input;

	/**
	 * @param input
	 * 		Input search query text.
	 */
	public FuzzySuggestionComparator(String input) {
		this.input = input;
	}

	@Override
	public int compare(Suggestion a, Suggestion b) {
		int cmp = Float.compare(
				FuzzySearch.ratio(b.getText(), input) / 100F,
				FuzzySearch.ratio(a.getText(), input) / 100F);
		if (cmp == 0)
			cmp = a.compareTo(b);
		return cmp;
	}
}
