package me.coley.recaf.search;

import jregex.Pattern;
import me.coley.recaf.util.Log;

import java.util.function.BiPredicate;

/**
 * String match implementations.
 *
 * @author Matt
 */
public enum StringMatchMode {
	/**
	 * String match via equality.
	 */
	EQUALS((key, text) -> text.equals(key)),
	/**
	 * String match via containment.
	 */
	CONTAINS((key, text) -> text.contains(key)),
	/**
	 * String match via same prefix.
	 */
	STARTS_WITH((key, text) -> text.startsWith(key)),
	/**
	 * String match via same suffix.
	 */
	ENDS_WITH((key, text) -> text.endsWith(key)),
	/**
	 * String match via regular expression matching.
	 */
	REGEX((key, text) -> regmatch(text, key));

	private static boolean regmatch(String text, String key) {
		try {
			return new Pattern(key).matcher(text).find();
		} catch(Exception ex) {
			Log.error(ex, "Invalid pattern: '{}'", key);
			return false;
		}
	}

	private final BiPredicate<String, String> matcher;

	StringMatchMode(BiPredicate<String, String> matcher)  {
		this.matcher = matcher;
	}

	/**
	 * @param key
	 * 		Expected pattern.
	 * @param text
	 * 		Text to test for a match.
	 *
	 * @return {@code true} if the given text matches with the given key.
	 */
	public boolean match(String key, String text) {
		return matcher.test(key, text);
	}
}
