package me.coley.recaf.ui.util;

import jregex.Matcher;
import me.coley.recaf.ui.behavior.Searchable;
import me.coley.recaf.util.RegexUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Search helper for reusable text search. Another class can implement {@link Searchable} and delegate its calls
 * to this, reducing clutter in the target class.
 *
 * @author Matt Coley
 */
public class SearchHelper implements Searchable {
	private final BiFunction<Integer, Integer, SearchResult> rangeToResult;
	private SearchResults lastResults;
	private String text;

	/**
	 * @param rangeToResult
	 * 		Mapper for ranges to result values.
	 */
	public SearchHelper(BiFunction<Integer, Integer, SearchResult> rangeToResult) {
		this.rangeToResult = rangeToResult;
	}

	/**
	 * @param newText
	 * 		New text content to search in.
	 */
	public void setText(String newText) {
		// Skip if the same
		if (newText.equals(text))
			return;
		// Set new text, invalidate prior results
		text = newText;
		lastResults = null;
	}

	@Override
	public SearchResults next(EnumSet<SearchModifier> modifiers, String search) {
		if (lastResults != null && lastResults.isSameParameters(modifiers, search)) {
			lastResults.next();
			return lastResults;
		}
		List<SearchResult> results = new ArrayList<>();
		populateSearch(results, modifiers, search);
		return lastResults = new SearchResults(results, modifiers, search);
	}

	@Override
	public SearchResults previous(EnumSet<SearchModifier> modifiers, String search) {
		if (lastResults != null && lastResults.isSameParameters(modifiers, search)) {
			lastResults.previous();
			return lastResults;
		}
		List<SearchResult> results = new ArrayList<>();
		populateSearch(results, modifiers, search);
		return lastResults = new SearchResults(results, modifiers, search);
	}

	private void populateSearch(List<SearchResult> results, EnumSet<SearchModifier> modifiers, String search) {
		if (text == null)
			return;
		if (modifiers.contains(SearchModifier.REGEX)) {
			Matcher matcher = RegexUtil.getMatcher(search, text);
			while (matcher.find()) {
				results.add(rangeToResult.apply(matcher.start(), matcher.end()));
			}
		} else {
			boolean checkWord = modifiers.contains(SearchModifier.WORD);
			boolean caseSensitive = modifiers.contains(SearchModifier.CASE_SENSITIVE);
			String text = this.text;
			if (!caseSensitive) {
				text = text.toLowerCase();
				search = search.toLowerCase();
			}
			int len = search.length();
			int index = text.indexOf(search);
			while (index >= 0) {
				if (!checkWord || (isBoundary(index - 1) && isBoundary(index + len))) {
					results.add(rangeToResult.apply(index, index + len));
				}
				// Find next
				index = text.indexOf(search, index + len);
			}
		}
	}

	/**
	 * @param index
	 * 		Some index in the {@link #text} to check.
	 * 		This is either the match index -1, or the index plus the matched text length.
	 * 		Because of this, text bounds checks count in addition to actual text boundary characters.
	 *
	 * @return {@code true} when the character at the position in the text is a boundary.
	 */
	private boolean isBoundary(int index) {
		return index >= text.length() || index < 0 || String.valueOf(text.charAt(index)).matches("\\W");
	}
}
