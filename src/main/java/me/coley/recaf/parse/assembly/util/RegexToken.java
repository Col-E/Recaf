package me.coley.recaf.parse.assembly.util;


import jregex.Matcher;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Linked list of regex matchers, allows to indicate which part of the regex chain failed.
 *
 * @author Matt
 */
public class RegexToken {
	private final String token;
	private final AbstractMatcher matcher;
	private final BiFunction<RegexToken, String, List<String>> suggestions;
	private RegexToken next, prev;
	private String found, lastTarget;

	private RegexToken(RegexToken prev, String name, AbstractMatcher matcher, BiFunction<RegexToken,
			String, List<String>> suggestions) {
		this.prev = prev;
		this.token = name;
		this.matcher = matcher;
		this.suggestions = suggestions;
	}

	/**
	 * @return Suggested strings based on last input.
	 */
	public List<String> suggest() {
		// Feed in the root token (so get(token) can be called) in addition to the last input
		return suggestions.apply(root(), lastTarget);
	}

	/**
	 * @return Root token,
	 */
	public RegexToken root() {
		if(prev == null)
			return this;
		return prev.root();
	}

	/**
	 * @return Name of this token.
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Get a token's matched text by its name.
	 *
	 * @param token
	 * 		Name of token in the sequence.
	 *
	 * @return Matched text of the token.
	 */
	public String get(String token) {
		if(token.equals(this.token))
			return found;
		else if(next != null)
			return next.get(token);
		return null;
	}

	/**
	 * Get a token's matched value by its name.
	 *
	 * @param token
	 * 		Name of token in the sequence.
	 *
	 * @return Matched value of the token.
	 */
	public <T> T getMatch(String token) {
		if(token.equals(this.token)) {
			// Attempt to use sub-class capable of casting
			if (matcher instanceof UniMatcher)
				return (T) ((UniMatcher)matcher).get();
			// Default to string, will throw cast-exception if context is not of a string.
			return (T) matcher.getMatcher().group(0);
		}
		else if(next != null)
			return next.getMatch(token);
		return null;
	}

	/**
	 * @param token
	 * 		Name of token in the sequence.
	 *
	 * @return {@code true} if token exists and has been found. {@code false} otherwise.
	 */
	public boolean has(String token) {
		return get(token) != null;
	}

	/**
	 * Appends another token to the sequence.
	 *
	 * @param token
	 * 		Name of the token.
	 * @param matcher
	 * 		Matcher to verify the token against input.
	 *
	 * @return
	 */
	public RegexToken append(String token, AbstractMatcher matcher, BiFunction<RegexToken, String,
			List<String>> suggestions) {
		next = new RegexToken(this, token, matcher, suggestions);
		return next;
	}

	/**
	 * Check the success result of {@link #matches(String)}.
	 *
	 * @param text
	 * 		Text to match
	 *
	 * @return {@code true} if all tokens matched. {@code false} otherwise.
	 */
	public boolean rawMatches(String text) {
		return matches(text).isSuccess();
	}

	/**
	 * Check if the given text matches the token <i>(and all following tokens)</i>.
	 *
	 * @param text
	 * 		Text to match
	 *
	 * @return MatchResult instance for detailed results <i>(success, failing token)</i>.
	 */
	public MatchResult matches(String text) {
		try {
			return new MatchResult(runMatch(text), null);
		} catch(MatchFail failure) {
			return new MatchResult(false, failure.cause);
		}
	}

	/**
	 * True implementation of {@link #matches(String)}.
	 *
	 * @param text
	 * 		Text to match
	 *
	 * @return {@code true} if all tokens matched.
	 *
	 * @throws MatchFail
	 * 		Thrown to log which token caused the faulure.
	 */
	private boolean runMatch(String text) throws MatchFail {
		lastTarget = text;
		// Check if text matches
		Matcher m = matcher.getMatcher();
		m.setTarget(text);
		if(m.find()) {
			found = m.group(0);
			// If this is the last token, done
			if(next == null)
				return true;
			// Check next token with the substring
			String sub = text.substring(m.end());
			return next.runMatch(sub);
		}
		throw new MatchFail(this);
	}

	/**
	 * @param token
	 * 		Name of the token.
	 * @param matcher
	 * 		Matcher to verify the token against input.
	 * @param suggestions
	 * 		Suggestion generator.
	 *
	 * @return New RegexToken.
	 */
	public static RegexToken create(String token, AbstractMatcher matcher, BiFunction<RegexToken,
			String, List<String>> suggestions) {
		return new RegexToken(null, token, matcher, suggestions);
	}

	/**
	 * Private exception to pass the offending token to the top-most caller of
	 * {@link #matches(String)}.
	 */
	private static class MatchFail extends Exception {
		RegexToken cause;

		MatchFail(RegexToken cause) {
			this.cause = cause;
		}
	}
}
