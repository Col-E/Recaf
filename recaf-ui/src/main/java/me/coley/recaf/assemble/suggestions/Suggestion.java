package me.coley.recaf.assemble.suggestions;

import me.coley.recaf.code.ItemInfo;

import javax.annotation.Nullable;
import java.util.BitSet;

/**
 * Suggestion entry for {@link SuggestionsResults}
 *
 * @author Matt Coley
 */
public class Suggestion implements Comparable<Suggestion> {
	private final ItemInfo info;
	private final String text;
	@Nullable
	private final BitSet matchedChars;

	/**
	 * @param info
	 * 		Associated item info of the suggestion.
	 * @param text
	 * 		Suggestion text.
	 * @param matchedChars
	 * 		Chars which match the search.
	 */
	public Suggestion(@Nullable ItemInfo info, String text, @Nullable BitSet matchedChars) {
		this.info = info;
		this.text = text;
		this.matchedChars = matchedChars;
	}

	/**
	 * @param info
	 * 		Associated item info of the suggestion.
	 * @param text
	 * 		Suggestion text.
	 */
	public Suggestion(@Nullable ItemInfo info, String text) {
		this(info, text, null);
	}

	/**
	 * May denote a class, field, or method depending on the context of the suggestion result.
	 *
	 * @return Associated item info of the suggestion.
	 * May be {@code null} when the suggestion does not relate to any of these items.
	 */
	@Nullable
	public ItemInfo getInfo() {
		return info;
	}

	/**
	 * @return Suggestion text.
	 */
	public String getText() {
		return text;
	}

	@Override
	public int compareTo(Suggestion o) {
		return text.compareTo(o.text);
	}

	@Nullable
	public BitSet getMatchedChars() {
		return matchedChars;
	}
}
