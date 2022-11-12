package me.coley.recaf.assemble.suggestions;

import me.xdrop.fuzzywuzzy.FuzzySearch;

import java.util.Comparator;

/**
 * Sorts strings based on a fuzzy match to the input search text.
 *
 * @author Matt Coley
 */
public class FuzzySuggestionComparator implements Comparator<String> {
	private final String input;

	/**
	 * @param input
	 * 		Input search query text.
	 */
	public FuzzySuggestionComparator(String input) {
		this.input = input;
	}

	@Override
	public int compare(String a, String b) {
		int cmp = Float.compare(
				FuzzySearch.ratio(b, input) / 100F,
				FuzzySearch.ratio(a, input) / 100F);
		if (cmp == 0)
			cmp = a.compareTo(b);
		return cmp;
	}
}